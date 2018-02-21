package com.whiker.learn.zk.impl;

import com.whiker.learn.common.Util;
import com.whiker.learn.zk.LeaderElection;
import com.whiker.learn.zk.ZkClient;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

public class ZkLeaderElection implements LeaderElection, Watcher {
    private static final Logger log = LoggerFactory.getLogger(ZkLeaderElection.class);

    private final ZkClient client;
    private final String leaderNodePath;
    private final String id;

    private final CopyOnWriteArrayList<LeaderChangeCallbackWrapper> subscribers = new CopyOnWriteArrayList<>();

    private volatile boolean isLeader = false;
    private volatile boolean isExited = true;

    public ZkLeaderElection(ZkClient client, String leaderNodePath, String id) {
        this.client = client;
        this.leaderNodePath = leaderNodePath;
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public synchronized void elect() {
        if (!isExited) {
            return;
        }
        isExited = false;
        ZkClient.Result result;

        while (!isExited) {
            result = client.createNode(leaderNodePath, true, false, id);
            if (result.code == ZkClient.ResultCode.Ok) {
                isLeader = true;
            } else if (result.code == ZkClient.ResultCode.NodeExist) {
                isLeader = false;
            } else {
                // 网络不通, 或server不可用, 继续重试创建node
                log.error("create znode exception. leaderNodePath={}", leaderNodePath, result.ex);
                continue;
            }

            result = client.getData(leaderNodePath, this);
            if (result.code == ZkClient.ResultCode.Ok) {
                // createNode()成功后可能断开, 导致节点被删除, 再调用getData()时得到其他机器的id, 因此需要加判断
                isLeader = id.equals(new String(result.data, Util.UTF8));
                break;
            }

            // 节点不存在, 或出现其他异常, 继续重试创建node
            isLeader = false;
            if (result.ex != null) {
                log.error("get znode data exception. leaderNodePath={}", leaderNodePath, result.ex);
            }
        }

        notify(new LeaderChangeEvent(isLeader, isExited));
    }

    @Override
    public synchronized void process(WatchedEvent event) {
        if (isExited) {
            return;
        }

        if (event.getType() == Event.EventType.NodeCreated
                || event.getType() == Event.EventType.NodeDataChanged) {
            ZkClient.Result result = client.getData(leaderNodePath, this);
            if (result.code == ZkClient.ResultCode.Ok) {
                isLeader = id.equals(new String(result.data, Util.UTF8));
                notify(new LeaderChangeEvent(isLeader, isExited));
                return;
            }
        }
        elect();
        notify(new LeaderChangeEvent(isLeader, isExited));
    }

    @Override
    public synchronized void exitElection() {
        isLeader = false;
        isExited = true;

        // 是主节点时删除临时节点
        ZkClient.Result result;
        while (isLeader && (result = client.delete(leaderNodePath)).code != ZkClient.ResultCode.Ok) {
            log.error("delete znode exception. leaderNodePath={}", leaderNodePath, result.ex);
        }

        notify(new LeaderChangeEvent(isLeader, isExited));
    }

    @Override
    public boolean isLeader() {
        return isLeader;
    }

    private void notify(LeaderChangeEvent event) {
        for (LeaderChangeCallbackWrapper subscriber : subscribers) {
            subscriber.onLeaderChanged(event);
        }
    }

    @Override
    public void subscribeLeaderChangedEvent(LeaderChangeCallback subscriber) {
        LeaderChangeCallbackWrapper cb = new LeaderChangeCallbackWrapper(subscriber);
        cb.onLeaderChanged(new LeaderChangeEvent(isLeader, isExited));
        subscribers.add(cb);
    }

    @Override
    public void unsubscribeLeaderChangedEvent(LeaderChangeCallback subscriber) {
        subscribers.remove(new LeaderChangeCallbackWrapper(subscriber));
    }

    private static final class LeaderChangeCallbackWrapper implements LeaderChangeCallback {
        private final LeaderChangeCallback callback;

        private LeaderChangeCallbackWrapper(LeaderChangeCallback callback) {
            this.callback = callback;
        }

        @Override
        public synchronized void onLeaderChanged(LeaderChangeEvent event) {
            try {
                callback.onLeaderChanged(event);
            } catch (Throwable t) {
                log.error("onLeaderChanged exception", t);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj != null && obj instanceof LeaderChangeCallbackWrapper
                    && callback.equals(((LeaderChangeCallbackWrapper) obj).callback));
        }

        @Override
        public int hashCode() {
            return callback.hashCode();
        }
    }
}
