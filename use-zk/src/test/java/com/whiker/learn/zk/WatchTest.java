package com.whiker.learn.zk;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * 2018/2/20
 */
public class WatchTest implements Watcher {
    private static final Logger log = LoggerFactory.getLogger(WatchTest.class);

    private volatile CountDownLatch latch1 = new CountDownLatch(1);
    private volatile CountDownLatch latch2 = new CountDownLatch(1);

    @Override
    public void process(WatchedEvent event) {
        log.info("event: {}", event);
        if (latch1 != null) {
            latch1.countDown();
            latch1 = null;
            return;
        }
        if (latch2 != null) {
            latch2.countDown();
            latch2 = null;
        }
    }

    @Test
    public void test() throws Exception {
        final String rootNode = "/zktest";
        final String watchNode = rootNode + "/watchtest";
        WatchTest test = new WatchTest();
        ZkClient.Result result;

        ZkClient client = ZkClient.newClient("127.0.0.1:2181");

        client.createNode(rootNode, false, false, "");
        client.createNode(watchNode, true, false, "test-1");

        result = client.getData(watchNode, test);
        Assert.assertEquals("test-1", result.getStringData());
        log.info("getData-1: {}", result);

        result = client.setData(watchNode, "test-2");
        Assert.assertEquals(ZkClient.ResultCode.Ok, result.code);
        test.latch1.await();

        result = client.getData(watchNode, test);
        Assert.assertEquals("test-2", result.getStringData());
        log.info("getData-2: {}", result);

        result = client.setData(watchNode, "test-3");
        Assert.assertEquals(ZkClient.ResultCode.Ok, result.code);
        test.latch2.await();

        result = client.getData(watchNode, null);
        Assert.assertEquals("test-3", result.getStringData());
        log.info("getData-3: {}", result);
    }
}
