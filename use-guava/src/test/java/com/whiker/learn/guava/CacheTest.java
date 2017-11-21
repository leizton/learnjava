package com.whiker.learn.guava;

import com.google.common.cache.*;
import com.google.common.collect.Lists;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class CacheTest {

    public static void main(String[] args) throws Exception {
        test1();
    }

    private static void test1() throws Exception {
        MyRemovalListener rmListener = new MyRemovalListener();
        LoadingCache<String, Set<Long>> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .removalListener(rmListener)
                .maximumSize(20)
                .build(new CacheLoader<String, Set<Long>>() {
                    @Override
                    public Set<Long> load(String key) throws Exception {
                        return new TreeSet<>();
                    }
                });
        rmListener.cache = cache;

        Set<Long> v1 = cache.get("001");
        v1.addAll(Lists.newArrayList(11L, 13L));

        Thread.sleep(1200);
        System.out.println(cache.get("001"));  // 死锁
    }

    private static final class MyRemovalListener implements RemovalListener<String, Set<Long>> {
        private LoadingCache<String, Set<Long>> cache;

        @Override
        public void onRemoval(RemovalNotification<String, Set<Long>> notification) {
            System.out.println("remove: " + notification.getKey() + ", " + notification.getValue());
            try {
                if (notification.getValue() != null) {
                    cache.get(notification.getKey()).addAll(notification.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void test2() throws Exception {
        LoadingCache<String, Set<Long>> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .removalListener(new RemovalListener<String, Set<Long>>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, Set<Long>> notification) {
                        System.out.println("remove: " + notification.getKey() + ", " + notification.getValue());
                    }
                })
                .maximumSize(20)
                .build(new CacheLoader<String, Set<Long>>() {
                    @Override
                    public Set<Long> load(String key) throws Exception {
                        return new TreeSet<>();
                    }
                });
        Set<Long> v1 = cache.get("001");
        Set<Long> v2 = cache.get("002");
        Set<Long> v3 = cache.get("001");
        v1.add(11L);
        v2.add(12L);
        v3.add(13L);

        Thread.sleep(1200);
        System.out.println(cache.size());      // size == 2, 时间过了但并没有删除
        System.out.println(cache.get("001"));  // 访问一下才删除过期key, 再load新的<"001",[]>
        System.out.println(cache.size());      // size == 1, <"001",[]>

        cache.get("001").add(101L);
        cache.get("002").add(102L);
        System.out.println(cache.size());      // size == 2
        Thread.sleep(1200);
        cache.get("003");
        System.out.println(cache.size());      // size == 2, 还没有超过maxSize(20), 此时清理一个过期key
        cache.get("004");
        System.out.println(cache.size());      // size == 3, 仍有一个过期key还没清理
        cache.get("005");
        System.out.println(cache.size());      // size == 3, 2个过期key都清理了
    }
}
