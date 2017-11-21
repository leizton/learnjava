package com.whiker.learn.common;

/**
 * 测量代码运行时间
 *
 * @author whiker@163.com create on 16-6-18.
 */
public class CostTimer {

    private long start; // 开始时间, 毫秒
    private long cost; // 用时, 毫秒

    public CostTimer() {
        start = System.currentTimeMillis();
    }

    public void start() {
        start = System.currentTimeMillis();
    }

    public long end() {
        cost = System.currentTimeMillis() - start;
        return cost;
    }

    public long getStart() {
        return start;
    }

    public long getCost() {
        return cost;
    }
}
