package com.whiker.learn.javabase.locker.model;

import com.whiker.learn.javabase.locker.AbstractQueuedSynchronizer;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

import static com.whiker.learn.javabase.locker.AbstractQueuedSynchronizer.interruptSelf;
import static com.whiker.learn.javabase.locker.AbstractQueuedSynchronizer.spinForNanosTimeoutThreshold;

public class ConditionObject implements Condition {

  private static final int THROW_IE = -1;    // wait退出时, throw InterruptedException
  private static final int REINTERRUPT = 1;  // wait退出时, interruptSelf()

  private static void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
    if (interruptMode == THROW_IE) {
      throw new InterruptedException();
    } else if (interruptMode == REINTERRUPT) {
      interruptSelf();
    }
  }

  private final AbstractQueuedSynchronizer syner;
  private Node firstWaiter, lastWaiter;  // conditionQueue

  public ConditionObject(AbstractQueuedSynchronizer syner) {
    this.syner = syner;
  }

  // conditionQueue的操作 ---------------------------------------------------------
  private Node addNewWaiter() {
    // 此时已经占有锁, 无需考虑线程安全
    if (lastWaiter != null && lastWaiter.waitStatus != Node.CONDITION) {
      unlinkCancelledWaiters();
    }

    // 单链表的尾插入
    Node node = new Node(Node.CONDITION);
    if (lastWaiter == null)
      firstWaiter = node;
    else
      lastWaiter.nextWaiter = node;
    lastWaiter = node;
    return node;
  }

  // 遍历单链表, 移出waitStatus不是Node.CONDITION的节点
  private void unlinkCancelledWaiters() {
    Node curr = firstWaiter, prev = null;
    while (curr != null) {
      Node next = curr.nextWaiter;
      if (curr.waitStatus != Node.CONDITION) {  // curr取消了条件等待
        curr.nextWaiter = null;  // help GC

        // 移出curr
        if (prev == null)
          firstWaiter = next;
        else
          prev.nextWaiter = next;

        if (next == null) {
          lastWaiter = prev;
          break;
        }
      } else {
        prev = curr;
      }
      curr = next;
    }
  }

  // Condition接口, await部分 -----------------------------------------------------
  public final void await() throws InterruptedException {
    // 此时已经占有锁
    if (Thread.interrupted())
      throw new InterruptedException();

    Node node = addNewWaiter();

    // 释放锁
    int savedState = syner.fullyRelease(node);

    // 等待被唤醒
    int interruptMode = 0;
    while (!syner.isOnSyncQueue(node)) {
      LockSupport.park(this);
      if ((interruptMode = isInterruptedWhileWaiting(node)) != 0)
        break;
    }

    // 重新加锁
    boolean interruptedWhileAcquier = syner.acquireQueued(node, savedState);
    if (interruptedWhileAcquier && interruptMode == 0)
      interruptMode = REINTERRUPT;

    if (node.nextWaiter != null)
      unlinkCancelledWaiters();  // 移出conditionQueue中cancelled的节点
    if (interruptMode != 0)
      reportInterruptAfterWait(interruptMode);
  }

  private int isInterruptedWhileWaiting(Node node) {
    return Thread.interrupted() ? (syner.transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
  }

  public final long awaitNanos(long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();

    Node node = addNewWaiter();

    // 释放锁
    int savedState = syner.fullyRelease(node);

    // 等待被唤醒, 或超时
    final long deadline = System.nanoTime() + nanosTimeout;
    int interruptMode = 0;
    while (!syner.isOnSyncQueue(node)) {
      if (nanosTimeout <= 0L) {
        // 取消条件等待
        syner.transferAfterCancelledWait(node);
        break;
      }

      // 超时时间的精度是spinForNanosTimeoutThreshold(1毫秒)
      if (nanosTimeout >= spinForNanosTimeoutThreshold)
        LockSupport.parkNanos(this, nanosTimeout);

      if ((interruptMode = isInterruptedWhileWaiting(node)) != 0)
        break;
      nanosTimeout = deadline - System.nanoTime();
    }

    // 重新加锁
    if (syner.acquireQueued(node, savedState) && interruptMode != THROW_IE)
      interruptMode = REINTERRUPT;

    if (node.nextWaiter != null)
      unlinkCancelledWaiters();
    if (interruptMode != 0)
      reportInterruptAfterWait(interruptMode);
    return nanosTimeout;
  }

  // return: 是否在超时之前收到signal
  // 不能直接通过await(long nanosTimeout)实现
  public boolean await(long time, TimeUnit unit) throws InterruptedException {
    long nanosTimeout = unit.toNanos(time);
    if (Thread.interrupted())
      throw new InterruptedException();
    Node node = addNewWaiter();
    int savedState = syner.fullyRelease(node);
    final long deadline = System.nanoTime() + nanosTimeout;
    boolean timedout = false;
    int interruptMode = 0;
    while (!syner.isOnSyncQueue(node)) {
      if (nanosTimeout <= 0L) {
        // transferAfterCancelledWait()返回true才表示超时
        // 不是通过 nanosTimeout<0 判断超时
        timedout = syner.transferAfterCancelledWait(node);
        break;
      }
      if (nanosTimeout >= spinForNanosTimeoutThreshold)
        LockSupport.parkNanos(this, nanosTimeout);
      if ((interruptMode = isInterruptedWhileWaiting(node)) != 0)
        break;
      nanosTimeout = deadline - System.nanoTime();
    }
    if (syner.acquireQueued(node, savedState) && interruptMode != THROW_IE)
      interruptMode = REINTERRUPT;
    if (node.nextWaiter != null)
      unlinkCancelledWaiters();
    if (interruptMode != 0)
      reportInterruptAfterWait(interruptMode);
    return !timedout;
  }

  public boolean awaitUntil(Date deadline) throws InterruptedException {
    return await(deadline.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  public final void awaitUninterruptibly() {
    throw new UnsupportedOperationException();
  }

  // Condition接口, signal部分 ----------------------------------------------------
  public final void signal() {
    // 必须已经占有锁
    if (!syner.isHeldExclusively())
      throw new IllegalMonitorStateException();

    // 唤醒conditionQueue中的等待节点
    while (firstWaiter != null) {
      Node toWakeup = firstWaiter;

      // 移出toWakeup
      firstWaiter = toWakeup.nextWaiter;
      toWakeup.nextWaiter = null;
      if (firstWaiter == null) lastWaiter = null;

      if (syner.transferForSignal(toWakeup)) {
        break;
      }
    }
  }

  public final void signalAll() {
    if (!syner.isHeldExclusively())
      throw new IllegalMonitorStateException();

    // 唤醒整个conditionQueue
    Node p = firstWaiter;
    firstWaiter = lastWaiter = null;
    while (p != null) {  // 遍历conditionQueue
      Node next = p.nextWaiter;
      p.nextWaiter = null;
      syner.transferForSignal(p);
      p = next;
    }
  }
}
