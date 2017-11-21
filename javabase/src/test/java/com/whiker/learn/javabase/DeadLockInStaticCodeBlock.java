package com.whiker.learn.javabase;

import java.util.concurrent.CountDownLatch;

public class DeadLockInStaticCodeBlock {

    /**
     * 存在死锁的static初始代码块
     */
    static {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            /**
             * 当前线程先加这个static块的锁，但不会成功
             * 因为这个static块卡在latch.await()没有退出，所以不会放锁
             */
            System.out.println("to count down");  // 不会执行
            latch.countDown();
        }).start();

        try {
            System.out.println("await");
            latch.await();
            System.out.println("await return");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
    }
}
