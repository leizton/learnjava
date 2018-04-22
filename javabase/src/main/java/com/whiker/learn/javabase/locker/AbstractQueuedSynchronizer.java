package com.whiker.learn.javabase.locker;

import com.whiker.learn.javabase.locker.model.Node;
import sun.misc.Unsafe;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractQueuedSynchronizer {

  public static final long spinForNanosTimeoutThreshold = 1000L;

  public static void interruptSelf() {
    Thread.currentThread().interrupt();
  }

  // 4个核心字段 ----------------------------------------------------------------------
  volatile Thread exclusiveOwnerThread;  // 当前持有同步器的线程, 用于实现可重入
  volatile int state;                    // 在ReenLock里表示acquireNum
  private volatile Node head, tail;      // waitQueue是CLH_LockQueue的变体, CLH锁是公平的自旋锁

  // waitQueue的操作 -----------------------------------------------------------------
  // mode: exclusive/shared
  private Node addNewWaiter(Node mode) {
    Node node = new Node(mode);
    enq(node);
    return node;
  }

  // node入队到waitQueue的(插入尾部), 并返回node的前驱
  private Node enq(Node node) {
    while (true) {
      Node t = tail;
      if (t == null) {
        // 初始化 waitQueue, head是空node
        if (casHead(null, new Node())) {
          tail = head;
        }
      } else {
        node.pred = t;
        if (casTail(t, node)) {
          t.next = node;
          return t;
        }
      }
    }
  }

  private void setHead(Node node) {
    head = node;
    node.thread = null;
    node.pred = null;
  }

  // 需要子类实现的部分 ----------------------------------------------------------------
  // 返回是否acquire成功
  boolean tryAcquire(int acquireNum) {
    throw new UnsupportedOperationException();
  }

  // 返回是否彻底释放了同步器
  boolean tryRelease(int releaseNum) {
    throw new UnsupportedOperationException();
  }

  int tryAcquireShared(int acquireNum) {
    throw new UnsupportedOperationException();
  }

  boolean tryReleaseShared(int releaseNum) {
    throw new UnsupportedOperationException();
  }

  public boolean isHeldExclusively() {
    throw new UnsupportedOperationException();
  }

  //--------------------------------------------------------------------------------
  /*
    acquire --> tryAcquire
            --> acquireQueued --> tryAcquire
                              --> shouldPark
                              --> park
                              <-- cancelAcquire
  */
  void acquire(int acquireNum) {
    if (tryAcquire(acquireNum)) {
      return;
    }
    Node node = addNewWaiter(Node.EXCLUSIVE);
    if (acquireQueued(node, acquireNum)) {
      interruptSelf();
    }
  }

  // 返回acquire过程中线程是否interrupted
  // 即使线程被interrupt, 也不会返回, 而是继续尝试加锁, 原因是new waiter已经加入到队列中
  public boolean acquireQueued(Node node, int acquireNum) {
    boolean interrupted = false;
    try {
      while (true) {
        Node pred = node.predecessor();
        if (pred == head && tryAcquire(acquireNum)) {
          setHead(node);
          pred.next = null;  // help GC
          return interrupted;
        }
        if (shouldParkAfterTryAcquireFailed(pred, node)) {
          interrupted = parkAndCheckInterrupt();
        }
      }
    } catch (Throwable t) {
      cancelAcquire(node);
      if (interrupted) {
        interruptSelf();
      }
      throw t;
    }
  }

  private boolean shouldParkAfterTryAcquireFailed(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL) {
      return true;
    }

    if (ws > 0) {
      // 前驱取消了
      do {
        node.pred = pred = pred.pred;  // 跳过取消了的前驱
      } while (pred.waitStatus > 0);  // todo 用ws==CANCELLED来判断是cancelled
      pred.next = node;
    } else {
      // 前驱未取消, 所以尝试把状态改成需要后继去park
      // 这个修改过程中可能出现pred被cancelled, 所以用cas, 并且本次不park
      pred.casWaitSt(ws, Node.SIGNAL);
    }
    return false;
  }

  // LockSupport.park(obj) 挂起当前线程,直到 LockSupport.unpark(thread)
  private boolean parkAndCheckInterrupt() {
    LockSupport.park(this);
    return Thread.interrupted();
  }

  private void cancelAcquire(Node node) {
    if (node == null) {
      return;
    }
    node.thread = null;

    // 跳过取消了的前驱
    while (node.pred.waitStatus > 0) {
      node.pred = node.pred.pred;
    }

    final Node pred = node.pred;
    final Node predNext = pred.next;
    node.waitStatus = Node.CANCELLED;  // 设置cancelled可以让后继node跳过本节点

    /*
      分3种情况:
      1. node是tail
      2. node是head.next
      3. node不是tail也不是head.next
    */
    if (node == tail && casTail(node, pred)) {
      // node是tail, 且把tail更新成了pred(如果enq()里把tail变成新节点, 则node不是tail)
      // casTail(node, pred)是把node从queue remove
      pred.casNext(predNext, null);
    } else if (pred == head) {
      // node是head.next, 通知后继
      unparkSuccessor(node);
    } else {
      final int ws = pred.waitStatus;
      if (ws > 0) {
        // pred was cancelled
        unparkSuccessor(node);
        return;
      }

      // node.thread = null 只发生在setHead()和cancelAcquire()里
      if (pred.casWaitSt(ws, Node.SIGNAL) && pred.thread != null) {
        // 把node从queue移出
        // 此处无需修改node.next的pred, 因为node.next会在shouldPark()里skip掉本节点
        Node next = node.next;
        if (next != null && next.waitStatus <= 0) {
          pred.casNext(predNext, next);
        }
        return;
      }
      unparkSuccessor(node);
    }
  }

  // 唤醒后继(不一定是next, 因为next可能cancelled)
  private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    if (ws < 0) {
      node.casWaitSt(ws, 0);
    }

    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
      // s is null or cancelled
      s = null;

      // 从tail往前找第一个没有被cancelled的节点
      for (Node t = tail; t != null && t != node; t = t.pred) {
        if (t.waitStatus <= 0) {
          s = t;
        }
      }
    }
    if (s != null) {
      LockSupport.unpark(s.thread);
    }
  }

  boolean tryAcquireNanos(int acquireNum, long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return tryAcquire(acquireNum) || doAcquireNanos(acquireNum, nanosTimeout);
  }

  // 与acquireQueued()类似的逻辑
  private boolean doAcquireNanos(int acquireNum, long timeout) throws InterruptedException {
    if (timeout <= 0L) {
      return false;
    }
    final long deadline = System.nanoTime() + timeout;
    final Node node = addNewWaiter(Node.EXCLUSIVE);
    try {
      while (true) {
        Node pred = node.predecessor();
        if (pred == head && tryAcquire(acquireNum)) {
          setHead(node);
          pred.next = null; // help GC
          return true;
        }

        timeout = deadline - System.nanoTime();
        if (timeout <= 0L) {
          return false;
        }
        if (shouldParkAfterTryAcquireFailed(pred, node) && timeout > spinForNanosTimeoutThreshold) {
          LockSupport.parkNanos(this, timeout);
        }
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
      }
    } catch (Throwable t) {
      cancelAcquire(node);
      throw t;
    }
  }

  boolean release(int releaseNum) {
    if (!tryRelease(releaseNum)) {
      return false;
    }

    Node h = head;
    if (h != null && h.waitStatus != 0) {
      unparkSuccessor(h);
    }
    return true;
  }

  // condition部分 --------------------------------------------------------------
  // 条件量await()时需要释放锁
  public int fullyRelease(Node node) {
    boolean failed = true;
    try {
      int savedState = state;
      if (release(savedState)) {
        failed = false;
        return savedState;
      } else {
        throw new IllegalMonitorStateException();
      }
    } finally {
      if (failed)
        node.waitStatus = Node.CANCELLED;
    }
  }

  // 判断node是否在waitQueue里
  public boolean isOnSyncQueue(Node node) {
    if (node.waitStatus == Node.CONDITION || node.pred == null) {
      // node在conditionQueue里, node.pred==null表示node占了锁
      return false;
    }

    if (node.next != null) {
      return true;
    }

    // 从tail往前找node, 找到则返回true, 否则返回false
    Node t = tail;
    while (t != null && t != node) {
      t = t.pred;
    }
    return t == node;
  }

  public boolean transferAfterCancelledWait(Node node) {
    if (node.casWaitSt(Node.CONDITION, 0)) {
      // 因超时或被中断, 而取消node的await(), 并把node转移到waitQueue里
      enq(node);
      return true;
    }

    // 虽然超时或被中断, 但node收到了signal()或signalAll()的唤醒
    while (!isOnSyncQueue(node))
      Thread.yield();
    return false;
  }

  // return: 是否唤醒node成功
  public boolean transferForSignal(Node node) {
    if (!node.casWaitSt(Node.CONDITION, 0)) {
      // node已经取消了await, 所以唤醒失败
      return false;
    }

    Node pred = enq(node);
    int ws = pred.waitStatus;
    if (ws > 0 || !pred.casWaitSt(ws, Node.SIGNAL)) {
      // node的前驱cancelled, 或cas改成SIGNAL失败(被cancelled了)
      LockSupport.unpark(node.thread);
    }
    return true;
  }

  // share部分 ------------------------------------------------------------------

  /**
   * Release action for shared mode -- signals successor and ensures
   * propagation. (Note: For exclusive mode, release just amounts
   * to calling unparkSuccessor of head if it needs signal.)
   */
  private void doReleaseShared() {
    /*
     * Ensure that a release propagates, even if there are other
     * in-progress acquires/releases.  This proceeds in the usual
     * way of trying to unparkSuccessor of head if it needs
     * signal. But if it does not, status is set to PROPAGATE to
     * ensure that upon release, propagation continues.
     * Additionally, we must loop in case a new node is added
     * while we are doing this. Also, unlike other uses of
     * unparkSuccessor, we need to know if CAS to reset status
     * fails, if so rechecking.
     */
    for (; ; ) {
      Node h = head;
      if (h != null && h != tail) {
        int ws = h.waitStatus;
        if (ws == Node.SIGNAL) {
          if (!h.casWaitSt(Node.SIGNAL, 0))
            continue;            // loop to recheck cases
          unparkSuccessor(h);
        } else if (ws == 0 &&
            !h.casWaitSt(0, Node.PROPAGATE))
          continue;                // loop on failed CAS
      }
      if (h == head)                   // loop if head changed
        break;
    }
  }

  /**
   * Sets head of queue, and checks if successor may be waiting
   * in shared mode, if so propagating if either propagate > 0 or
   * PROPAGATE status was set.
   *
   * @param node      the node
   * @param propagate the return value from a tryAcquireShared
   */
  private void setHeadAndPropagate(Node node, int propagate) {
    Node h = head; // Record old head for check below
    setHead(node);
    /*
     * Try to signal next queued node if:
     *   Propagation was indicated by caller,
     *     or was recorded (as h.waitStatus either before
     *     or after setHead) by a previous operation
     *     (note: this uses sign-check of waitStatus because
     *      PROPAGATE status may transition to SIGNAL.)
     * and
     *   The next node is waiting in shared mode,
     *     or we don't know, because it appears null
     *
     * The conservatism in both of these checks may cause
     * unnecessary wake-ups, but only when there are multiple
     * racing acquires/releases, so most need signals now or soon
     * anyway.
     */
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
      Node s = node.next;
      if (s == null || s.isShared())
        doReleaseShared();
    }
  }

  /**
   * Acquires in shared uninterruptible mode.
   *
   * @param arg the acquire argument
   */
  private void doAcquireShared(int arg) {
    final Node node = addNewWaiter(Node.SHARED);
    boolean failed = true;
    try {
      boolean interrupted = false;
      for (; ; ) {
        final Node p = node.predecessor();
        if (p == head) {
          int r = tryAcquireShared(arg);
          if (r >= 0) {
            setHeadAndPropagate(node, r);
            p.next = null; // help GC
            if (interrupted) {
              interruptSelf();
            }
            failed = false;
            return;
          }
        }
        if (shouldParkAfterTryAcquireFailed(p, node) && parkAndCheckInterrupt())
          interrupted = true;
      }
    } finally {
      if (failed)
        cancelAcquire(node);
    }
  }

  /**
   * Acquires in shared interruptible mode.
   *
   * @param arg the acquire argument
   */
  private void doAcquireSharedInterruptibly(int arg)
      throws InterruptedException {
    final Node node = addNewWaiter(Node.SHARED);
    boolean failed = true;
    try {
      for (; ; ) {
        final Node p = node.predecessor();
        if (p == head) {
          int r = tryAcquireShared(arg);
          if (r >= 0) {
            setHeadAndPropagate(node, r);
            p.next = null; // help GC
            failed = false;
            return;
          }
        }
        if (shouldParkAfterTryAcquireFailed(p, node) && parkAndCheckInterrupt())
          throw new InterruptedException();
      }
    } finally {
      if (failed)
        cancelAcquire(node);
    }
  }

  private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
      throws InterruptedException {
    if (nanosTimeout <= 0L)
      return false;
    final long deadline = System.nanoTime() + nanosTimeout;
    final Node node = addNewWaiter(Node.SHARED);
    boolean failed = true;
    try {
      for (; ; ) {
        final Node p = node.predecessor();
        if (p == head) {
          int r = tryAcquireShared(arg);
          if (r >= 0) {
            setHeadAndPropagate(node, r);
            p.next = null; // help GC
            failed = false;
            return true;
          }
        }
        nanosTimeout = deadline - System.nanoTime();
        if (nanosTimeout <= 0L)
          return false;
        if (shouldParkAfterTryAcquireFailed(p, node) &&
            nanosTimeout > spinForNanosTimeoutThreshold)
          LockSupport.parkNanos(this, nanosTimeout);
        if (Thread.interrupted())
          throw new InterruptedException();
      }
    } finally {
      if (failed)
        cancelAcquire(node);
    }
  }

  /**
   * Releases in exclusive mode.  Implemented by unblocking one or
   * more threads if {@link #tryRelease} returns true.
   * This method can be used to implement method {@link Lock#unlock}.
   *
   * @param arg the release argument.  This value is conveyed to
   *            {@link #tryRelease} but is otherwise uninterpreted and
   *            can represent anything you like.
   * @return the value returned from {@link #tryRelease}
   */

  /**
   * Acquires in shared mode, ignoring interrupts.  Implemented by
   * first invoking at least once {@link #tryAcquireShared},
   * returning on success.  Otherwise the thread is queued, possibly
   * repeatedly blocking and unblocking, invoking {@link
   * #tryAcquireShared} until success.
   *
   * @param arg the acquire argument.  This value is conveyed to
   *            {@link #tryAcquireShared} but is otherwise uninterpreted
   *            and can represent anything you like.
   */
  public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
      doAcquireShared(arg);
  }

  /**
   * Acquires in shared mode, aborting if interrupted.  Implemented
   * by first checking interrupt status, then invoking at least once
   * {@link #tryAcquireShared}, returning on success.  Otherwise the
   * thread is queued, possibly repeatedly blocking and unblocking,
   * invoking {@link #tryAcquireShared} until success or the thread
   * is interrupted.
   *
   * @param arg the acquire argument.
   *            This value is conveyed to {@link #tryAcquireShared} but is
   *            otherwise uninterpreted and can represent anything
   *            you like.
   * @throws InterruptedException if the current thread is interrupted
   */
  public final void acquireSharedInterruptibly(int arg)
      throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
      doAcquireSharedInterruptibly(arg);
  }

  /**
   * Attempts to acquire in shared mode, aborting if interrupted, and
   * failing if the given timeout elapses.  Implemented by first
   * checking interrupt status, then invoking at least once {@link
   * #tryAcquireShared}, returning on success.  Otherwise, the
   * thread is queued, possibly repeatedly blocking and unblocking,
   * invoking {@link #tryAcquireShared} until success or the thread
   * is interrupted or the timeout elapses.
   *
   * @param arg          the acquire argument.  This value is conveyed to
   *                     {@link #tryAcquireShared} but is otherwise uninterpreted
   *                     and can represent anything you like.
   * @param nanosTimeout the maximum number of nanoseconds to wait
   * @return {@code true} if acquired; {@code false} if timed out
   * @throws InterruptedException if the current thread is interrupted
   */
  public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
      throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();
    return tryAcquireShared(arg) >= 0 ||
        doAcquireSharedNanos(arg, nanosTimeout);
  }

  /**
   * Releases in shared mode.  Implemented by unblocking one or more
   * threads if {@link #tryReleaseShared} returns true.
   *
   * @param arg the release argument.  This value is conveyed to
   *            {@link #tryReleaseShared} but is otherwise uninterpreted
   *            and can represent anything you like.
   * @return the value returned from {@link #tryReleaseShared}
   */
  public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
      doReleaseShared();
      return true;
    }
    return false;
  }

  // Queue inspection methods

  /**
   * Queries whether any threads are waiting to acquire. Note that
   * because cancellations due to interrupts and timeouts may occur
   * at any time, a {@code true} return does not guarantee that any
   * other thread will ever acquire.
   * <p>
   * <p>In this implementation, this operation returns in
   * constant time.
   *
   * @return {@code true} if there may be other threads waiting to acquire
   */
  public final boolean hasQueuedThreads() {
    return head != tail;
  }

  /**
   * Queries whether any threads have ever contended to acquire this
   * synchronizer; that is if an acquire method has ever blocked.
   * <p>
   * <p>In this implementation, this operation returns in
   * constant time.
   *
   * @return {@code true} if there has ever been contention
   */
  public final boolean hasContended() {
    return head != null;
  }

  /**
   * Returns the first (longest-waiting) thread in the queue, or
   * {@code null} if no threads are currently queued.
   * <p>
   * <p>In this implementation, this operation normally returns in
   * constant time, but may iterate upon contention if other threads are
   * concurrently modifying the queue.
   *
   * @return the first (longest-waiting) thread in the queue, or
   * {@code null} if no threads are currently queued
   */
  public final Thread getFirstQueuedThread() {
    // handle only fast path, else relay
    return (head == tail) ? null : fullGetFirstQueuedThread();
  }

  /**
   * Version of getFirstQueuedThread called when fastpath fails
   */
  private Thread fullGetFirstQueuedThread() {
    /*
     * The first node is normally head.next. Try to get its
     * thread field, ensuring consistent reads: If thread
     * field is nulled out or s.pred is no longer head, then
     * some other thread(s) concurrently performed setHead in
     * between some of our reads. We try this twice before
     * resorting to traversal.
     */
    Node h, s;
    Thread st;
    if (((h = head) != null && (s = h.next) != null &&
        s.pred == head && (st = s.thread) != null) ||
        ((h = head) != null && (s = h.next) != null &&
            s.pred == head && (st = s.thread) != null))
      return st;

    /*
     * Head's next field might not have been set yet, or may have
     * been unset after setHead. So we must check to see if tail
     * is actually first node. If not, we continue on, safely
     * traversing from tail back to head to find first,
     * guaranteeing termination.
     */

    Node t = tail;
    Thread firstThread = null;
    while (t != null && t != head) {
      Thread tt = t.thread;
      if (tt != null)
        firstThread = tt;
      t = t.pred;
    }
    return firstThread;
  }

  // 返回是否有其他thread排在currThread前面
  boolean hasQueuedPredecessors() {
    // 必须先读取tail, 再读取head, 读取顺序与enq()里初始化顺序相反
    Node t = tail;
    Node h = head;
    Node s;
    // h != t, 说明waitQueue非空
    // h.next != null, h.next不是当前线程的waiter节点, 说明有前驱
    return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
  }

  boolean apparentlyFirstQueuedIsExclusive() {
    Node h, s;
    return (h = head) != null && (s = h.next) != null && !s.isShared() && s.thread != null;
  }

  private static final Unsafe unsafe = Unsafe.getUnsafe();
  private static final long stateOffset;
  private static final long headOffset;
  private static final long tailOffset;

  static {
    try {
      stateOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("state"));
      headOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("head"));
      tailOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
    } catch (Exception ex) {
      throw new Error(ex);
    }
  }

  private boolean casHead(Node expect, Node update) {
    return unsafe.compareAndSwapObject(this, headOffset, expect, update);
  }

  private boolean casTail(Node expect, Node update) {
    return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
  }

  boolean casState(int expect, int update) {
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
  }
}
