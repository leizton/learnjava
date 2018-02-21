package com.whiker.learn.zk.taskexec;

import com.whiker.learn.zk.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2018/2/20
 */
public class TaskSubmit {
    private static final Logger log = LoggerFactory.getLogger(TaskSubmit.class);

    private final ZkClient client;

    public TaskSubmit(ZkClient client) {
        this.client = client;
    }

    public boolean submit(String taskData) {
        ZkClient.Result result = client.createNode(Constants.zkTaskChildNodeName, false, true, taskData);
        if (result.code != ZkClient.ResultCode.Ok) {
            log.error("submit error. taskData={}, result={}", taskData, result);
            return false;
        }
        return true;
    }
}
