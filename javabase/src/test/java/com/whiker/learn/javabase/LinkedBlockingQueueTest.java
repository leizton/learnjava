package com.whiker.learn.javabase;

import com.whiker.learn.common.TestUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yiqun.fan create on 17-12-22.
 */
public class LinkedBlockingQueueTest {

    @Test
    public void testDrainTo() throws Exception {
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        AtomicBoolean end = new AtomicBoolean(false);

        AtomicInteger count = new AtomicInteger(0);
        Thread t1 = new Thread(() -> {
            while (count.get() < 10000) {
                queue.offer(count.getAndIncrement());
                TestUtil.sleep(5);
            }
            end.set(true);
        });

        AtomicInteger error = new AtomicInteger();
        Random random = new Random();
        Thread t2 = new Thread(() -> {
            int globalMax = -1;
            while (!end.get()) {
                List<Integer> list = new ArrayList<>(queue.size());
                queue.drainTo(list);
                if (list.isEmpty()) {
                    TestUtil.sleep(5 + random.nextInt(60));
                    continue;
                }

                for (int i = 1; i < list.size(); i++) {
                    if (list.get(i) != list.get(i - 1) + 1) {
                        error.getAndIncrement();
                    }
                }
                int localMin = list.get(0);
                int localMax = list.get(list.size() - 1);
                if (localMin != globalMax + 1 || list.size() != (localMax - globalMax)) {
                    error.getAndIncrement();
                }

                globalMax = localMax;
                System.out.println(list.size());
                TestUtil.sleep(25 + random.nextInt(20));
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("error num: " + error);
    }
}
