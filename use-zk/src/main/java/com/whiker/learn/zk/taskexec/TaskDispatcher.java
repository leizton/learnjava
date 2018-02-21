package com.whiker.learn.zk.taskexec;

import com.whiker.learn.common.ConcurrentHashSet;
import com.whiker.learn.common.Util;
import com.whiker.learn.zk.LeaderElection;
import com.whiker.learn.zk.ZkClient;
import com.whiker.learn.zk.impl.ZkLeaderElection;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class TaskDispatcher implements LeaderElection.LeaderChangeCallback, Runnable {
    private static final Logger log = LoggerFactory.getLogger(TaskDispatcher.class);

    private static final int STOPED_STATE = 0;
    private static final int RUNNING_STATE = 1;
    private static final int STOPPING_STATE = 2;
    private static final List<String> EMPTY_CHILDREN = new ArrayList<>();
    private static final Random RANDOM = new Random();

    private final ZkClient client;
    private volatile LeaderElection leaderElection;

    private volatile Thread runnerThread;
    private final LinkedBlockingQueue<Runnable> runners = new LinkedBlockingQueue<>();
    private volatile CountDownLatch runnerStopLatch;

    private final AtomicInteger state = new AtomicInteger(STOPED_STATE);
    private volatile boolean isRunning = true;

    private final ConcurrentHashMap<String, Worker> workers = new ConcurrentHashMap<>();
    private final ConcurrentSkipListSet<String> todoTaskNodeSet = new ConcurrentSkipListSet<>();
    private final ConcurrentHashSet<String> doingTaskNodes = new ConcurrentHashSet<>();
    private final ReentrantLock workersUpdateLock = new ReentrantLock();
    private final ReentrantLock tasksUpdateLock = new ReentrantLock();

    private volatile boolean isLeader = false;

    public TaskDispatcher(ZkClient client) {
        this.client = client;
        init();
    }

    private void init() {
        createNode(Constants.zkRootNode);
        createNode(Constants.zkWorkersNode);
        createNode(Constants.zkTasksNode);

        new ChildNodesUpdater(Constants.zkWorkersNode, this::workersUpdate);
        new ChildNodesUpdater(Constants.zkTasksNode, this::tasksUpdate);
    }

    private void createNode(String path) {
        ZkClient.Result result = client.createNode(path, false, false);
        if (result.code != ZkClient.ResultCode.Ok && result.code != ZkClient.ResultCode.NodeExist) {
            throw new RuntimeException("create znode exception, path=" + path, result.ex);
        }
    }

    public boolean start() {
        if (!state.compareAndSet(STOPED_STATE, RUNNING_STATE)) {
            return false;
        }

        leaderElection = new ZkLeaderElection(client, Constants.zkDispatcherNode, Util.generateId());
        leaderElection.elect();
        leaderElection.subscribeLeaderChangedEvent(this);

        runnerStopLatch = new CountDownLatch(1);
        isRunning = true;
        (runnerThread = new Thread(this)).start();
        return true;
    }

    public void stop() {
        if (state.compareAndSet(RUNNING_STATE, STOPPING_STATE)) {
            isRunning = false;
            runnerThread.interrupt();
            runnerThread = null;
            Util.awaitIgnoreInterrupted(runnerStopLatch);
            runnerStopLatch = null;

            leaderElection.exitElection();
            leaderElection = null;

            state.set(STOPED_STATE);
        }
    }

    @Override
    public void onLeaderChanged(LeaderElection.LeaderChangeEvent event) {
        isLeader = !event.isExitElect && event.isLeader;
        log.info("leader changed. isLeader={}", isLeader);
    }

    private void workersUpdate(List<String> workerNodes) {
        workersUpdateLock.lock();
        try {
            // 删除已下线的worker
            for (Map.Entry<String, Worker> e : workers.entrySet()) {
                if (!workerNodes.contains(e.getKey())) {
                    workers.remove(e.getKey());
                }
            }

            // 添加新上线的worker
            for (String workNode : workerNodes) {
                if (!workers.containsKey(workNode)) {
                    workers.put(workNode, new Worker(workNode));
                }
            }
        } finally {
            workersUpdateLock.unlock();
        }
    }

    private void tasksUpdate(List<String> taskNodeList) {
        tasksUpdateLock.lock();
        try {
            // 删除取消的task
            Set<String> taskNodes = new HashSet<>(taskNodeList);
            for (String todo : todoTaskNodeSet) {
                if (!taskNodes.contains(todo)) {
                    todoTaskNodeSet.remove(todo);
                }
            }

            // 添加新的task
            for (String taskNode : taskNodes) {
                if (!doingTaskNodes.contains(taskNode)) {
                    if (!todoTaskNodeSet.contains(taskNode)) {
                        todoTaskNodeSet.add(taskNode);
                        log.info("new task: {}", taskNode);
                    }
                }
            }
            runners.offer(this::dispatch);
        } finally {
            tasksUpdateLock.unlock();
        }
    }

    private void dispatch() {
        if (!isLeader || todoTaskNodeSet.isEmpty()) {
            return;
        }

        workersUpdateLock.lock();
        try {
            Optional<Worker> idleWorker = selectAnIdleWorker();
            if (!idleWorker.isPresent()) {
                return;
            }

            String todoTask = todoTaskNodeSet.first();
            DispatchResult dispatchResult = idleWorker.get().dispatch(todoTask);
            if (dispatchResult == DispatchResult.Ok || dispatchResult == DispatchResult.TaskCancelled) {
                doingTaskNodes.add(todoTask);  // 阻止tasksUpdate()里把todoTask放到todoTaskNodeSet
                todoTaskNodeSet.pollFirst();
                log.info("task dispatched: {}", todoTask);
            }
        } finally {
            workersUpdateLock.unlock();
        }
    }

    private Optional<Worker> selectAnIdleWorker() {
        List<Worker> idleWorkers = new LinkedList<>();
        for (Worker worker : workers.values()) {
            if (Constants.workerIdleNodeData.equals(worker.data)) {
                idleWorkers.add(worker);
            }
        }
        if (idleWorkers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(idleWorkers.get(RANDOM.nextInt(idleWorkers.size())));
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Runnable runner = runners.take();
                if (runner != null) {
                    runner.run();
                }
            } catch (InterruptedException e) {
                log.warn("run task be interrupted");
            } catch (Exception e) {
                log.error("run task exception", e);
            }
        }
        runnerStopLatch.countDown();
    }

    private final class ChildNodesUpdater implements Watcher {

        private final String path;
        private final Consumer<List<String>> updateCb;

        private ChildNodesUpdater(String path, Consumer<List<String>> updateCb) {
            this.path = path;
            this.updateCb = updateCb;
            update();
        }

        @Override
        public void process(WatchedEvent event) {
            if (!isRunning) {
                return;
            }
            switch (event.getType()) {
                case NodeChildrenChanged:
                    update();
                    break;
                default:
                    log.error("TaskDispatcher.ChildNodesUpdater unknow state. path={}, event={}", path, event);
                    updateCb.accept(EMPTY_CHILDREN);
                    break;
            }
        }

        private void update() {
            List<String> children = new ArrayList<>();
            ZkClient.Result result = client.getChildren(path, this, children);
            if (result.code == ZkClient.ResultCode.Ok) {
                updateCb.accept(children);
            } else {
                log.info("TaskDispatcher.ChildNodesUpdater getChildren. path={}, result={}", path, result);
                updateCb.accept(EMPTY_CHILDREN);
                runners.offer(this::update);  // retry
            }
        }
    }

    private final class Worker implements Watcher {
        private final String path;
        private volatile String data;
        private volatile String taskNodePath;

        private Worker(String nodeName) {
            this.path = Constants.zkWorkersNode + "/" + nodeName;
            getData();
        }

        @Override
        public void process(WatchedEvent event) {
            switch (event.getType()) {
                case None:
                case NodeDeleted:
                    // 由workers的ChildNodesUpdater的回调处理
                    log.info("worker offline. path={}", path);
                    break;
                case NodeDataChanged:
                    getData();
                    break;
                default:
                    log.error("TaskDispatcher.Worker unknow state. path={}, event={}", path, event);
                    break;
            }
        }

        private void getData() {
            ZkClient.Result result = client.getData(path, this);
            if (result.code == ZkClient.ResultCode.Ok) {
                data = result.getStringData();
                if (Constants.workerIdleNodeData.equals(data)) {
                    runners.offer(TaskDispatcher.this::dispatch);
                }
            } else if (result.code == ZkClient.ResultCode.NoNode) {
                // 由workers的ChildNodesUpdater的回调处理
                log.warn("TaskDispatcher.Worker getData but no znode. path={}", path);
            } else {
                log.error("TaskDispatcher.Worker getData unknow state. path={}, result={}", path, result, result.ex);
            }
        }

        private boolean setData(String data) {
            ZkClient.Result result = client.setData(path, data);
            if (result.code == ZkClient.ResultCode.Ok) {
                return true;
            }
            if (result.code == ZkClient.ResultCode.NoNode) {
                log.info("worker offline. path={}", path);
            } else {
                log.error("TaskDispatcher.Worker setData error. path={}, result={}", path, result, result.ex);
            }
            return false;
        }

        private DispatchResult dispatch(String todoTaskNode) {
            taskNodePath = Constants.zkTasksNode + "/" + todoTaskNode;
            if (setData(taskNodePath)) {
                return DispatchResult.Ok;
            }
            taskNodePath = null;
            return DispatchResult.Error;
        }
    }

    private enum DispatchResult {
        Ok, Error, TaskCancelled
    }
}
