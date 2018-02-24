package com.whiker.learn.javabase.quesync;

/**
 * 2018/2/24
 */
public class AbstractOwnableSynchronizer {

    protected AbstractOwnableSynchronizer() {
    }

    private transient Thread exclusiveOwnerThread;

    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
