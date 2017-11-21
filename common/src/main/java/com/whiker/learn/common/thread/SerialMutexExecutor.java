package com.whiker.learn.common.thread;

import java.util.concurrent.*;

/**
 * @author yiqun.fan@qunar.com create on 16-6-11.
 *         串行互斥执行器
 */
public class SerialMutexExecutor {

    /**
     * 创建没有rejected处理的串行互斥执行器
     */
    public static ThreadPoolExecutor newExecutor() {
        return new SingleThreadExecutor(new ArrayBlockingQueue<Runnable>(1, true));
    }

    /**
     * 创建带rejected处理的串行互斥执行器
     */
    public static ThreadPoolExecutor newExecutorWithRejected(RejectedExecutionHandler rh) {
        return new SingleThreadExecutor(new ArrayBlockingQueue<Runnable>(1, true), rh);
    }

    /**
     * afterExecute()方法里清空Runnable队列
     */
    private static class SingleThreadExecutor extends ThreadPoolExecutor {
        private BlockingQueue<Runnable> workQueue;

        SingleThreadExecutor(BlockingQueue<Runnable> workQueue) {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, workQueue, new RejectedExecutionHandler() {
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                }
            });
            this.workQueue = workQueue;
        }

        SingleThreadExecutor(BlockingQueue<Runnable> workQueue, RejectedExecutionHandler rh) {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, workQueue, rh);
            this.workQueue = workQueue;
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            workQueue.clear();
        }
    }
}
