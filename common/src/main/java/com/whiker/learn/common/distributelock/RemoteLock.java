package com.whiker.learn.common.distributelock;

import com.google.common.base.Strings;
import com.whiker.learn.common.NetUtil;
import redis.clients.jedis.JedisCommands;
import sun.misc.Unsafe;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 提供进程锁的功能.
 * <p>
 * 不可重入;
 * 线程不安全, 同一个RemoteLock实例的lock()/unlock()等方法不是线程安全的;
 * <p>
 * 可以每次进入临界区前都创建一个新的RemoteLock实例, 这种做法同时保证了线程和进程的安全, 但牺牲了部分性能.
 * 每次新RemoteLock实例的nickName都应该不同, 而remoteLockName相同.
 * <p>
 * Created by whiker on 2017/3/14.
 */
public class RemoteLock {
    private static final String RedisKeyPrefix = "dlock_";

    private static String NickNamePrefix;
    private static AtomicInteger RemoteLockId = new AtomicInteger(0);
    private static Lock IdLock = new ReentrantLock();

    static {
        String ipAddress;
        try {
            Optional<String> opt = NetUtil.localIp();
            if (!opt.isPresent() || Strings.isNullOrEmpty(opt.get())) {
                throw new RuntimeException("get local ipAddress fail.");
            }
            ipAddress = opt.get();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        String processFlag = ManagementFactory.getRuntimeMXBean().getName();
        if (Strings.isNullOrEmpty(processFlag)) {
            throw new RuntimeException("get process flag error.");
        }

        NickNamePrefix = ipAddress + ":" + processFlag + ":";
    }

    private static int newRemoteLockId() {
        int id = RemoteLockId.getAndIncrement();
        if (id < 0) {
            if (RemoteLockId.get() < 0) {
                IdLock.lock();
                try {
                    if (RemoteLockId.get() < 0) {
                        RemoteLockId.set(0);
                    }
                } finally {
                    IdLock.unlock();
                }
            }
            id = RemoteLockId.getAndIncrement();
        }
        return id;
    }

    private static int PollLockingTimeInSecond = 1;
    private static int HeartBeatTimeInSecond = 60;
    private static int RemoteLockKeyExpireTimeInSecond = HeartBeatTimeInSecond + 10;

    public static void setPollLockingTimeInSecond(int pollLockingTimeInSecond) {
        PollLockingTimeInSecond = pollLockingTimeInSecond;
    }

    public static void setHeartBeatAndRemoteLockExpireTimeInSecond(int heartBeatTimeInSecond, int remoteLockKeyExpireTimeInSecond) {
        HeartBeatTimeInSecond = heartBeatTimeInSecond;
        RemoteLockKeyExpireTimeInSecond = remoteLockKeyExpireTimeInSecond;
    }

    private final JedisCommands client;
    private final String remoteLockKey;
    private final String involvedNodesKey;
    private final String nickName;
    private final boolean needMonitor;

    private volatile HeartBeatTask.LockedNode lockedNode;

    public RemoteLock(JedisCommands client, String remoteLockName) {
        this(client, remoteLockName, false);
    }

    public RemoteLock(JedisCommands client, String remoteLockName, boolean needMonitor) {
        this(client, remoteLockName, NickNamePrefix + newRemoteLockId(), false, needMonitor);
    }

    public RemoteLock(JedisCommands client, String remoteLockName, String nickName, boolean needMonitor) {
        this(client, remoteLockName, nickName, false, needMonitor);
    }

    private RemoteLock(JedisCommands client, String remoteLockName, String nickName, boolean needNickPrefix, boolean needMonitor) {
        this.client = client;
        this.remoteLockKey = RedisKeyPrefix + remoteLockName;
        this.involvedNodesKey = RedisKeyPrefix + remoteLockName + "_set";
        this.nickName = needNickPrefix ? NickNamePrefix + nickName : nickName;
        this.needMonitor = needMonitor;
    }

    public void lock() throws RemoteLockException, InterruptedException {
        if (needMonitor) {
            client.sadd(involvedNodesKey, nickName);
        }

        for (; ; ) {
            long setnxRet = client.setnx(remoteLockKey, nickName);
            client.expire(remoteLockKey, RemoteLockKeyExpireTimeInSecond);
            if (setnxRet == 0) {
                // poll locking
                for (; ; ) {
                    Thread.sleep(1000 * PollLockingTimeInSecond);
                    long ttlRet = client.ttl(remoteLockKey);
                    if (ttlRet == -2) {
                        // remoteLockKey is expired or deleted
                        break;
                    } else if (ttlRet == -1) {
                        // someone has not done expire
                        client.expire(remoteLockKey, RemoteLockKeyExpireTimeInSecond);
                    } else if (ttlRet < 0) {
                        throw new RemoteLockException("ttl return '" + ttlRet + "' unknown.");
                    }
                }
            } else if (setnxRet == 1) {
                break;
            } else {
                throw new RemoteLockException("setnx return '" + setnxRet + "' unknown.");
            }
        }
        // locked
        lockedNode = HeartBeatTask.add(client, remoteLockKey, nickName);
    }

    public boolean tryLock() throws RemoteLockException {
        long setnxRet = client.setnx(remoteLockKey, nickName);
        if (setnxRet != 1) {
            return false;
        }

        // locked
        client.expire(remoteLockKey, RemoteLockKeyExpireTimeInSecond);
        lockedNode = HeartBeatTask.add(client, remoteLockKey, nickName);
        if (needMonitor) {
            client.sadd(involvedNodesKey, nickName);
        }
        return true;
    }

    public boolean tryLock(int timeoutSeconds) throws RemoteLockException {
        if (timeoutSeconds <= 0) {
            return false;
        }
        if (needMonitor) {
            client.sadd(involvedNodesKey, nickName);
        }
        if (tryLock()) {
            return true;
        }

        LockPollTask.LockingNode node = LockPollTask.add(client, remoteLockKey, nickName, timeoutSeconds);
        try {
            if (node.latch.await(timeoutSeconds + 1, TimeUnit.SECONDS)) {
                if (node.timeoutInSencond >= 0 && nickName.equals(client.get(remoteLockKey))) {
                    node.client.expire(node.remoteLockKey, RemoteLockKeyExpireTimeInSecond);
                    lockedNode = HeartBeatTask.add(client, remoteLockKey, nickName);
                    return true;
                }
            }
        } catch (InterruptedException e) {
            // while node.latch.await(), the thread is interrupted
        }
        node.dead();
        return false;
    }

    public void unlock() throws RemoteLockException {
        lockedNode.dead();
        client.del(remoteLockKey);
        if (lockedNode.ex != null) {
            lockedNode = null;
            throw lockedNode.ex;
        }
        lockedNode = null;

        if (needMonitor) {
            client.srem(involvedNodesKey, nickName);
        }
    }

    private static ScheduledExecutorService scheduledExecutors = Executors.newScheduledThreadPool(2);

    private static class HeartBeatTask {

        private static class LockedNode extends LiveNode {
            final JedisCommands client;
            final String remoteLockKey;
            final String nickName;
            volatile RemoteLockException ex = null;

            LockedNode(JedisCommands client, String remoteLockKey, String nickName) {
                this.client = client;
                this.remoteLockKey = remoteLockKey;
                this.nickName = nickName;
            }
        }

        static LiveNodeList<LockedNode> lockedNodes = new LiveNodeList<>();

        static LockedNode add(JedisCommands client, String remoteLockKey, String nickName) {
            LockedNode node = new LockedNode(client, remoteLockKey, nickName);
            lockedNodes.add(node);
            return node;
        }

        static {
            scheduledExecutors.scheduleAtFixedRate(() -> {
                Iterator<LockedNode> it = lockedNodes.iterator();
                while (it.hasNext()) {
                    LockedNode node = it.next();
                    if (!node.nickName.equals(node.client.get(node.remoteLockKey))) {
                        // heart beat timeout
                        node.ex = new RemoteLockException("lost lock before unlock.");
                        it.remove();
                    }
                    if (node.client.expire(node.remoteLockKey, RemoteLockKeyExpireTimeInSecond) == 0) {
                        // has unlocked
                        it.remove();
                    }
                }
            }, 1, HeartBeatTimeInSecond, TimeUnit.SECONDS);
        }
    }

    private static class LockPollTask {

        private static class LockingNode extends LiveNode {
            final JedisCommands client;
            final String remoteLockKey;
            final String nickName;
            final CountDownLatch latch = new CountDownLatch(1);
            volatile int timeoutInSencond;

            private LockingNode(JedisCommands client, String remoteLockKey, String nickName, int timeoutInSencond) {
                this.client = client;
                this.remoteLockKey = remoteLockKey;
                this.nickName = nickName;
                this.timeoutInSencond = timeoutInSencond;
            }
        }

        static LiveNodeList<LockingNode> lockingNodes = new LiveNodeList<>();

        static LockingNode add(JedisCommands client, String remoteLockKey, String nickName, int timeoutInSencond) {
            LockingNode node = new LockingNode(client, remoteLockKey, nickName, timeoutInSencond);
            lockingNodes.add(node);
            return node;
        }

        static {
            scheduledExecutors.scheduleAtFixedRate(() -> {
                Iterator<LockingNode> it = lockingNodes.iterator();
                while (it.hasNext()) {
                    LockingNode node = it.next();
                    if (--node.timeoutInSencond < 0) {
                        // timeout
                        node.latch.countDown();
                        it.remove();
                        continue;
                    }

                    long ttlRet = node.client.ttl(node.remoteLockKey);
                    if (ttlRet == -2 && !node.isDead) {
                        long setnxRet = node.client.setnx(node.remoteLockKey, node.nickName);
                        if (setnxRet == 1) {
                            if (node.isDead) {
                                node.client.del(node.remoteLockKey);
                            } else {
                                node.client.expire(node.remoteLockKey, RemoteLockKeyExpireTimeInSecond);
                                node.latch.countDown();
                            }
                            it.remove();
                        }
                    } else if (ttlRet == -1) {
                        node.client.expire(node.remoteLockKey, RemoteLockKeyExpireTimeInSecond);
                    }
                }
            }, 1, PollLockingTimeInSecond, TimeUnit.SECONDS);
        }
    }

    public static class RemoteLockException extends Throwable {

        private RemoteLockException(String msg) {
            super(msg);
        }

        private RemoteLockException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    private static class LiveNode {
        static final Unsafe unsafe;
        static final long nextOffset;

        static {
            try {
                unsafe = Unsafe.getUnsafe();
                Field f = LiveNode.class.getDeclaredField("next");
                nextOffset = unsafe.objectFieldOffset(f);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        volatile boolean isDead = false;
        volatile LiveNode next = null;

        boolean casNext(LiveNode cmp, LiveNode val) {
            return unsafe.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        void dead() {
            isDead = true;
        }
    }

    // 等同于用ConcurrentLinkedQueue做add(e)，用LinkedList做遍历
    private static class LiveNodeList<E extends LiveNode> {
        final LiveNode head = new LiveNode();

        void add(LiveNode e) {
            if (e == null) {
                throw new IllegalArgumentException("the argument 'e' is null");
            }
            LiveNode oldFirst;
            do {
                oldFirst = head.next;
                e.next = oldFirst;
            } while (!head.casNext(oldFirst, e));
        }

        Iterator<E> iterator() {
            LiveNode nullNode = new LiveNode();
            nullNode.dead();
            add(nullNode);
            return new Iterator<E>() {
                LiveNode prev, curr = nullNode;

                @Override
                public boolean hasNext() {
                    while (curr.next != null && curr.next.isDead) {
                        curr.next = curr.next.next;
                    }
                    return curr.next != null;
                }

                @SuppressWarnings("unchecked")
                @Override
                public E next() {
                    LiveNode next = curr.next;
                    prev = curr;
                    curr = next;
                    return (E) next;
                }

                @Override
                public void remove() {  // must call next() ahead
                    prev.next = curr.next;
                    curr = prev;
                }
            };
        }
    }
}
