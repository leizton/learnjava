package com.whiker.learn.zk.taskexec;

public class Constants {

    public static final String zkServer = "127.0.0.1:2181";

    public static final String zkRootNode = "/zktest";
    public static final String zkDispatcherNode = zkRootNode + "/dispatcher";
    public static final String zkWorkersNode = zkRootNode + "/workers";
    public static final String zkTasksNode = zkRootNode + "/tasks";
    public static final String zkTaskChildNodeName = zkTasksNode + "/task-";

    public static final String workerIdleNodeData = "idle";
}
