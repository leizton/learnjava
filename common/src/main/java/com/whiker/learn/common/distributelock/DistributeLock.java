package com.whiker.learn.common.distributelock;

import redis.clients.jedis.JedisCommands;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Created by whiker on 2017/3/14.
 */
public class DistributeLock {

    private final Lock threadLock;
    private final RemoteLock remoteLock;

    public DistributeLock(Lock threadLock, JedisCommands client, String remoteLockName) {
        this(threadLock, client, remoteLockName, false);
    }

    public DistributeLock(Lock threadLock, JedisCommands client, String remoteLockName, boolean needMonitor) {
        this.threadLock = threadLock;
        remoteLock = new RemoteLock(client, remoteLockName, needMonitor);
    }

    public DistributeLock(Lock threadLock, JedisCommands client, String remoteLockName, String remoteLockNickName, boolean needMonitor) {
        this.threadLock = threadLock;
        remoteLock = new RemoteLock(client, remoteLockName, remoteLockNickName, needMonitor);
    }

    public void lock() throws RemoteLock.RemoteLockException, InterruptedException {
        threadLock.lock();
        try {
            remoteLock.lock();
        } catch (RemoteLock.RemoteLockException | InterruptedException e) {
            threadLock.unlock();
            throw e;
        }
    }

    public boolean tryLock() throws RemoteLock.RemoteLockException {
        return threadLock.tryLock() && remoteLock.tryLock();
    }

    public boolean tryLock(int timeoutSeconds) throws InterruptedException, RemoteLock.RemoteLockException {
        long time = System.currentTimeMillis();
        return threadLock.tryLock(timeoutSeconds, TimeUnit.SECONDS) &&
                remoteLock.tryLock(timeoutSeconds - (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - time));
    }

    public void unlock() throws RemoteLock.RemoteLockException {
        try {
            remoteLock.unlock();
        } finally {
            threadLock.unlock();
        }
    }
}
