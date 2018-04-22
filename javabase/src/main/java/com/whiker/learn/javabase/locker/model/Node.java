package com.whiker.learn.javabase.locker.model;

import sun.misc.Unsafe;

public class Node {

  public static final Node SHARED = new Node();
  public static final Node EXCLUSIVE = null;

  // todo 改成enum
  public static final int CANCELLED = 1;
  public static final int SIGNAL    = -1;  // 节点处于获取到独占锁状态或需要获取锁状态. 因此后继节点应该park, 等待本节点去unpark他
  public static final int CONDITION = -2;
  public static final int PROPAGATE = -3;

  public volatile Thread thread;
  public volatile int waitStatus;
  public Node nextWaiter;

  public volatile Node pred;
  public volatile Node next;

  public Node() {
  }

  public Node(Node mode) {
    thread = Thread.currentThread();
    waitStatus = 0;
    nextWaiter = mode;
  }

  public Node(int ws) {
    thread = Thread.currentThread();
    waitStatus = ws;
    nextWaiter = null;
  }

  public Node predecessor() throws NullPointerException {
    Node p = pred;
    if (p == null)
      throw new NullPointerException();
    else
      return p;
  }

  public boolean isShared() {
    return nextWaiter == SHARED;
  }

  public boolean casWaitSt(int expect, int update) {
    return unsafe.compareAndSwapInt(this, waitStatusOffset, expect, update);
  }

  public boolean casNext(Node expect, Node update) {
    return unsafe.compareAndSwapObject(this, nextOffset, expect, update);
  }

  private static final Unsafe unsafe = Unsafe.getUnsafe();
  private static final long waitStatusOffset;
  private static final long nextOffset;

  static {
    try {
      waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
      nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
    } catch (Exception ex) {
      throw new Error(ex);
    }
  }
}
