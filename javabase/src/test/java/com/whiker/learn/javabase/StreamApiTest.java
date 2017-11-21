package com.whiker.learn.javabase;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author whiker@163.com create on 16-7-16.
 */
public class StreamApiTest {

    @Test
    public void testFilter() {
        List<Integer> box = Lists.newArrayList(1, 2, 3, 4, 5, 6);
        // 过滤出偶数
        box = box.stream().filter((Integer i) -> (i & 1) == 0).collect(Collectors.toList());
        box.forEach(System.out::println);
    }

    @Test
    public void testMap() {
        List<Integer> radius = Lists.newArrayList(1, 2, 3, 4);
        // 计算平方
        List<Double> area = radius.stream().map((Integer r) -> 3.14 * r * r).collect(Collectors.toList());
        System.out.println(area);
        // double无法精确表示的数
        double area3 = 28.26;
        System.out.println(area3);
    }

    @Test
    public void testDistinctAndToset() {
        List<Integer> box = Lists.newArrayList(1, 1, 3, 3, 5, 5, 4, 4, 2, 2);

        List<Integer> boxList = box.stream().distinct().collect(Collectors.toList());
        System.out.println(boxList);

        Set<Integer> boxSet = box.stream().collect(Collectors.toSet());
        System.out.println(boxSet);
    }

    @Test
    public void testSkipAndLimit() {
        List<Integer> box = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8);
        final int pageSize = 3, pageNum = 1;
        box = box.stream().skip(pageNum * pageSize).limit(pageSize).collect(Collectors.toList());
        box.forEach(System.out::println);
    }

    @Test
    public void testFlatMap() {
        List<Integer> boxA = Lists.newArrayList(1, 3, 5);
        List<Integer> boxB = Lists.newArrayList(2, 4);
        List<int[]> pairs = boxA.stream()
                .flatMap(a -> boxB.stream().map(b -> new int[]{a, b}))
                .collect(Collectors.toList());
        pairs.forEach((int[] pair) -> System.out.println(pair[0] + ", " + pair[1]));
    }

    @Test
    public void testReduce() {
        List<Integer> box = Lists.newArrayList(1, 2, 3, 4, 5, 6);
        int sum = box.stream().reduce(0, (Integer sumTemp, Integer v) -> {
            System.out.print(sumTemp + ", ");
            return sumTemp + v;
        });
        System.out.println("累和: " + sum);
        int mul = box.stream().reduce(1, (Integer mulTemp, Integer v) -> {
            System.out.print(mulTemp + ", ");
            return mulTemp * v;
        });
        System.out.println("累积: " + mul);
    }

    @Test
    public void testArrayStream() {
        int[] box = new int[]{1, 2, 3, 4, 5, 6};
        List<Integer> boxList = Arrays.stream(box)
                .filter((int i) -> (i & 1) == 0)
                .mapToObj((i) -> i)
                .collect(Collectors.toList());
        boxList.forEach(System.out::println);
    }

    @Test
    public void testMaxBy() {
        List<Integer> box = Lists.newArrayList(1, 2, 3, 4, 5, 6);
        Comparator<Integer> intComp = Comparator.comparingInt(Integer::intValue);
        Optional<Integer> max = box.stream().collect(Collectors.maxBy(intComp));
        System.out.println(max);
        int sum = box.stream().collect(Collectors.summingInt(Integer::intValue));
        System.out.println(sum);
    }

    @Test
    public void testCompare() {
        List<Apple> apples = Lists.newArrayList();
        for (int i = 0; i < 10; i++) apples.add(new Apple());
        for (int i = 1; i < 10; i += 2) apples.get(i).weight = apples.get(i - 1).weight;
        System.out.println("原始: " + apples);

        // java.util.Comparator.comparing 和 方法引用
        apples.sort(Comparator.comparing(Apple::getWeight));
        System.out.println("weight顺序: " + apples);

        // Comparator.reverse()
        apples.sort(Comparator.comparing(Apple::getWeight).reversed());
        System.out.println("weight逆序: " + apples);

        // Comparator.thenComparing()
        apples.sort(Comparator.comparing(Apple::getWeight).reversed()
                .thenComparing(Apple::getColor, (c1, c2) -> c1.ordinal() - c2.ordinal()));
        System.out.println("weight/color: " + apples);
    }

    private static class Apple {
        private int weight;
        private Color color;

        Apple() {
            this.weight = new Random().nextInt(100);
            this.color = Color.values()[Math.abs(new Random().nextInt()) % Color.values().length];
        }

        int getWeight() {
            return weight;
        }

        Color getColor() {
            return color;
        }

        @Override
        public String toString() {
            return "(" + weight + ", " + color + ")";
        }

        private enum Color {
            Red, Cyan, Green, Yellow
        }
    }

    @Test
    public void testCaptureLambda() throws Exception {  // lambda捕获自由变量
        final int num = 10;  // 自由变量必须是final
        new Thread(() -> System.out.println(num)).start();
        Thread.sleep(500);
        System.out.println(num * num);
    }

    @Test
    public void test() {
        Runnable r = () -> System.out.println("test");
        Object o = r;
        System.out.println(o);
    }
}
