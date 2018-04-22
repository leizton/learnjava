package com.whiker.learn.javabase.locker;

import com.google.common.base.Preconditions;
import com.whiker.learn.javabase.locker.model.ConditionObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class ReenLock implements java.util.concurrent.locks.Lock {

  private final Sync sync;

  public ReenLock(boolean fair) {
    sync = new Sync(!fair);
  }

  public void lock() {
    sync.acquire();
  }

  public void lockInterruptibly() {
    throw new UnsupportedOperationException();
  }

  public boolean tryLock() {
    return sync.tryAcquire(true, 1);
  }

  public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
    return sync.tryAcquireNanos(1, unit.toNanos(timeout));
  }

  public void unlock() {
    sync.release(1);
  }

  public Condition newCondition() {
    return sync.newCondition();
  }

  private static final class Sync extends AbstractQueuedSynchronizer {
    final boolean isNonfair;

    Sync(boolean isNonfair) {
      this.isNonfair = isNonfair;
    }

    boolean tryAcquire(int acquireNum) {
      return tryAcquire(isNonfair, acquireNum);
    }

    boolean tryAcquire(boolean isNonfair, int acquireNum) {
      Preconditions.checkArgument(acquireNum > 0, "acquireNum must > 0");
      int n = state;
      if (n == 0) {
        if ((isNonfair || !hasQueuedPredecessors()) && casState(0, acquireNum)) {
          exclusiveOwnerThread = Thread.currentThread();
          return true;
        }
        // if CAS fail, 说明被其他线程抢占了
        return false;
      }

      if (isHeldExclusively()) {
        // 可重入
        int next = n + acquireNum;
        if (next < 0) {
          throw new Error("lock count overflow");
        }
        state = next;
        return true;
      }
      return false;
    }

    boolean tryRelease(int releaseNum) {
      if (!isHeldExclusively()) {
        throw new IllegalMonitorStateException();
      }

      int left = state - releaseNum;
      if (left == 0) {
        exclusiveOwnerThread = null;
      }

      // 这里直接改state是线程安全的, 因为tryRelease()前必须经过acquire(), 所以其他线程卡在acquire()上
      state = left;
      return left == 0;
    }

    public boolean isHeldExclusively() {
      return Thread.currentThread() == exclusiveOwnerThread;
    }

    void acquire() {
      // nonfair锁可以直接去抢所有权
      if (isNonfair && casState(0, 1)) {
        exclusiveOwnerThread = Thread.currentThread();
      } else {
        acquire(1);
      }
    }

    Condition newCondition() {
      return new ConditionObject(this);
    }
  }
}
