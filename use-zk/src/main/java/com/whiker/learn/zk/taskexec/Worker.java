package com.whiker.learn.zk.taskexec;

import com.whiker.learn.common.Util;
import com.whiker.learn.zk.ZkClient;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker implements Watcher {
    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    private final String workerName = Util.generateId();
    private final String workerNodePath = Constants.zkWorkersNode + "/" + workerName;
    private final ZkClient client;
    private final TaskExecutor executor;

    public Worker(ZkClient client, TaskExecutor executor) {
        this.client = client;
        this.executor = executor;
    }

    public void online() {
        ZkClient.Result result = client.createNode(workerNodePath, true, false, Constants.workerIdleNodeData);
        if (result.code != ZkClient.ResultCode.Ok && result.code != ZkClient.ResultCode.NodeExist) {
            throw new RuntimeException("create worker znode exception, path=" + workerNodePath, result.ex);
        }

        // watch
        result = client.getData(workerNodePath, this);
        if (result.code != ZkClient.ResultCode.Ok) {
            throw new RuntimeException("get worker znode data exception, path=" + workerNodePath, result.ex);
        }
        if (!Constants.workerIdleNodeData.equals(result.getStringData())) {
            exec(result.getStringData());
        }
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
            case NodeDeleted:
                log.error("worker znode lost. event={}", event);
                break;
            case NodeDataChanged:
                onDataChange();
                break;
            default:
                break;
        }
    }

    private void onDataChange() {
        ZkClient.Result result = client.getData(workerNodePath, this);
        if (result.code == ZkClient.ResultCode.Ok) {
            String taskNodePath = result.getStringData();
            if (!Constants.workerIdleNodeData.equals(taskNodePath)) {
                exec(taskNodePath);
            }
        } else {
            // TODO retry online
        }
    }

    private synchronized void exec(String taskNodePath) {
        ZkClient.Result result = client.getData(taskNodePath, null);
        if (result.code == ZkClient.ResultCode.Ok) {
            executor.exec(result.data);
            result = client.delete(taskNodePath);
            if (result.code != ZkClient.ResultCode.Ok) {
                log.error("delete task node error. taskNodePath={}, result={}", taskNodePath, result, result.ex);
            }
        } else if (result.code == ZkClient.ResultCode.NoNode) {
            log.info("task cancelled. taskNodePath={}", taskNodePath);
        } else {
            // TODO retry getData
            log.error("get task data error. taskNodePath={}, result={}", taskNodePath, result, result.ex);
        }
        setIdle();
    }

    private void setIdle() {
        ZkClient.Result result = client.setData(workerNodePath, Constants.workerIdleNodeData);
        if (result.code != ZkClient.ResultCode.Ok) {
            // TODO retry online
            log.error("setIdle error. path={}, result={}", workerNodePath, result, result.ex);
        }
    }
}
