package com.whiker.learn.zk.taskexec;

import com.whiker.learn.zk.ZkClient;

import java.util.concurrent.CountDownLatch;

/**
 * 2018/2/20
 */
public class TaskDispatcherTest {

    public static void main(String[] args) throws Exception {
        ZkClient zkClient = ZkClient.newClient(Constants.zkServer);
        TaskDispatcher dispatcher = new TaskDispatcher(zkClient);
        dispatcher.start();

        TaskExecutor executor = new PrintTaskExecutor();

        Worker worker = new Worker(zkClient, executor);
        worker.online();

        TaskSubmit taskSubmit = new TaskSubmit(zkClient);
        taskSubmit.submit("test-1");
        taskSubmit.submit("test-2");

        new CountDownLatch(1).await();
    }
}
