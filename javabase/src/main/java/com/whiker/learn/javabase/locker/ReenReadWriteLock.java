package com.whiker.learn.javabase.locker;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class ReenReadWriteLock implements ReadWriteLock {

  public Lock readLock() {
    return null;
  }

  public Lock writeLock() {
    return null;
  }

  private static final class HoldCounter {
    final long tid = Thread.currentThread().getId();
    int count = 0;
  }

  private static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
    public HoldCounter get() {
      return new HoldCounter();
    }
  }

  private static abstract class Sync extends AbstractQueuedSynchronizer {

    // high 16 位存sharedCount(共享count, 读)
    // low  16 位存exclusiveCount(独占count, 写)
    static final int SHARED_SHIFT = 16;
    static final int SHARED_UNIT = (1 << SHARED_SHIFT);  // 加共享锁时state的增量
    static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
    static final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;

    static int sharedCount(int c) {
      return (c >>> SHARED_SHIFT);
    }

    static int exclusiveCount(int c) {
      return (c & EXCLUSIVE_MASK);
    }

    private ThreadLocalHoldCounter readHolds;
    private HoldCounter cachedHoldCounter;
    private Thread firstReader;
    private int firstReaderHoldCount;

    // 加写锁, 或同时加写锁和读锁
    boolean tryAcquire(int acquireNum) {
      int n = state;
      if (n == 0) {
        if (!shouldExclusiveBlock() && casState(0, n + acquireNum)) {
          exclusiveOwnerThread = Thread.currentThread();
          return true;
        }
        return false;
      }

      int wn = exclusiveCount(n);
      if (wn == 0) {
        // n != 0 && w == 0, 说明共享锁被占不能加独占锁
        return false;
      }
      if (isHeldExclusively()) {
        // 是当前线程持有写锁
        if (wn + exclusiveCount(acquireNum) > MAX_COUNT) {
          throw new Error("exceed max lock count");
        }
        state = n + acquireNum;
        return true;
      }
      return false;
    }

    int tryAcquireShared(int acquireNum) {
      Thread currTh = Thread.currentThread();
      int n = state, wn = exclusiveCount(n);
      if (wn != 0 && !isHeldExclusively()) {
        return -1;
      }

      // wn == 0
      int rn = sharedCount(n);
      if (!shouldSharedBlock() && rn < MAX_COUNT && casState(n, n + SHARED_UNIT)) {
        // shared lock success
        if (rn == 0) {
          firstReader = currTh;
          firstReaderHoldCount = 1;
        } else if (firstReader == currTh) {
          firstReaderHoldCount++;
        } else {
          HoldCounter rh = cachedHoldCounter;
          if (rh == null || rh.tid != currTh.getId()) {
            cachedHoldCounter = rh = readHolds.get();
          } else if (rh.count == 0) {
            readHolds.set(rh);
          }
          rh.count++;
        }
        return 1;
      }
      return fullTryAcquireShared(currTh);
    }

    int fullTryAcquireShared(Thread currTh) {
      HoldCounter rh = null;
      while (true) {
        final int n = state, wn = exclusiveCount(n), rn = sharedCount(n);
        if (rn == MAX_COUNT) {
          throw new Error("exceed max lock count");
        }

        if (wn != 0) {
          if (exclusiveOwnerThread != currTh) {
            return -1;
          }
          // else we hold the exclusive lock; blocking here
          // would cause deadlock.
        } else if (shouldSharedBlock()) {
          // Make sure we're not acquiring read lock reentrantly
          if (firstReader != currTh) {
            if (rh == null) {
              rh = cachedHoldCounter;
              if (rh == null || rh.tid != currTh.getId()) {
                rh = readHolds.get();
                if (rh.count == 0)
                  readHolds.remove();
              }
            }
            if (rh.count == 0) {
              return -1;
            }
          }
        }

        if (casState(n, n + SHARED_UNIT)) {
          if (rn == 0) {
            firstReader = currTh;
            firstReaderHoldCount = 1;
          } else if (firstReader == currTh) {
            firstReaderHoldCount++;
          } else {
            if (rh == null)
              rh = cachedHoldCounter;
            if (rh == null || rh.tid != currTh.getId())
              rh = readHolds.get();
            else if (rh.count == 0)
              readHolds.set(rh);
            rh.count++;
            cachedHoldCounter = rh; // cache for release
          }
          return 1;
        }
      }
    }

    public boolean isHeldExclusively() {
      return Thread.currentThread() == exclusiveOwnerThread;
    }

    abstract boolean shouldExclusiveBlock();

    abstract boolean shouldSharedBlock();
  }

  private static final class NonfairSync extends Sync {
    boolean shouldExclusiveBlock() {
      return false;
    }

    boolean shouldSharedBlock() {
      // 队列里有加写锁时需要让给写锁
      return apparentlyFirstQueuedIsExclusive();
    }
  }

  private static final class FairSync extends Sync {
    boolean shouldExclusiveBlock() {
      return hasQueuedPredecessors();
    }

    boolean shouldSharedBlock() {
      return hasQueuedPredecessors();
    }
  }
}
