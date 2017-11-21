package com.whiker.learn.javabase;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * Created by whiker on 2017/3/29.
 */
public class ObjectWatiNotifyTest {

    private static long waitStart, waitEnd;
    private static long waitTimeoutStart, waitTimeoutEnd;

    private static void waitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() throws InterruptedException {
        String a = "str";
        CountDownLatch latch = new CountDownLatch(2);

        // wait
        Runnable r1 = () -> {
            System.out.println("wait");
            latch.countDown();
            waitLatch(latch);
            waitStart = System.currentTimeMillis();
            synchronized (a) {
                try {
                    a.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            waitEnd = System.currentTimeMillis();
        };
        new Thread(r1).start();

        // wait timeout
        Runnable r2 = () -> {
            System.out.println("wait timeout");
            latch.countDown();
            waitLatch(latch);
            waitTimeoutStart = System.currentTimeMillis();
            synchronized (a) {
                try {
                    a.wait(1500);  // 超时不会抛异常
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            waitTimeoutEnd = System.currentTimeMillis();
        };
        new Thread(r2).start();

        waitLatch(latch);
        for (int i = 3; i > 0; i--) {
            System.out.println(i);
            Thread.sleep(1000);
        }

        // notify
        synchronized (a) {
            a.notifyAll();
        }

        Thread.sleep(1000);
        System.out.println(waitEnd - waitStart);
        System.out.println(waitTimeoutEnd - waitTimeoutStart);
    }
}
