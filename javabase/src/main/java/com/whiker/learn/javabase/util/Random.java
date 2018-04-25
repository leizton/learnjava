package com.whiker.learn.javabase.util;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

/**
 * 2018/4/25
 */
public class Random {
  private static final java.util.Random RANDOM = new java.util.Random();

  public static void main(String[] args) {
    int[] bins = new int[10];
    for (int i = 0; i < 10000; i++) {
      bins[rand(10)]++;
    }
    System.out.println(Ints.asList(bins));
  }

  // java.util.Random.nextInt(int bound)
  // 用 RANDOM.nextInt(1 << 30) 模拟 RANDOM.next(30)
  @SuppressWarnings("NumericOverflow")
  private static int rand(final int bound) {
    Preconditions.checkArgument(bound > 0);

    final int m = bound - 1;
    int u, r;

    if ((bound & m) == 0) {  // bound is pow of 2
      u = RANDOM.nextInt(1 << 30);
      return (int) ((bound * (long) u) >> 31);  // 取u高位的log2(bound)个bit
    }

    do {
      u = RANDOM.nextInt(1 << 30);
      r = u % bound;
    } while (u + m < r);
    return r;
  }
}
