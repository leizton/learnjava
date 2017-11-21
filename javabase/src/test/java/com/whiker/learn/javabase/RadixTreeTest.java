package com.whiker.learn.javabase;

import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.Node;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

import java.util.Set;
import java.util.TreeSet;

public class RadixTreeTest {

    private static final MyTree<String> tree = new MyTree<>();

    static {
        tree.put("a", "1");
        tree.put("ab", "12");
        tree.put("abc", "123");
        tree.put("abd", "125");
        tree.put("abcd", "1234");
        tree.put("ah", "16");
        tree.put("ahi", "167");
        tree.put("ahj", "168");
        tree.put("ahik", "1679");
    }

    public static void main(String[] args) {
        test1();
        test2();
    }

    private static void test1() {
        tree.getKeysStartingWith("ab").forEach(cs -> System.out.println(String.valueOf(cs)));
        System.out.println();
        tree.getKeyValuePairsForKeysStartingWith("ab").forEach(kv -> System.out.println(String.valueOf(kv.getKey()) + "=" + kv.getValue()));
        System.out.println();
    }

    private static void test2() {
        tree.getPrefixNodes("abcd").forEach(kv -> System.out.println(String.valueOf(kv.getKey()) + "=" + kv.getValue()));
        System.out.println();
        tree.getPrefixNodes("ahik").forEach(kv -> System.out.println(String.valueOf(kv.getKey()) + "=" + kv.getValue()));
    }

    private static final class MyTree<E> extends ConcurrentRadixTree<E> {
        MyTree() {
            super(new DefaultCharArrayNodeFactory());
        }

        @SuppressWarnings("unchecked")
        Set<KeyValuePair<E>> getPrefixNodes(final String s) {
            Set<KeyValuePair<E>> ret = new TreeSet<>();
            Node node = super.root;
            for (int matched = 0; matched < s.length(); ++matched) {
                node = node.getOutgoingEdge(s.charAt(matched));
                if (node == null) {
                    break;
                }
                ret.add(new MyKeyValuePair<>(s.substring(0, matched + 1), (E) node.getValue()));
            }
            return ret;
        }

        private static final class MyKeyValuePair<E> implements KeyValuePair<E>, Comparable<MyKeyValuePair<E>> {
            private final CharSequence key;
            private final E value;

            MyKeyValuePair(String key, E value) {
                this.key = key;
                this.value = value;
            }

            @Override
            public CharSequence getKey() {
                return key;
            }

            @Override
            public E getValue() {
                return value;
            }

            @Override
            public int compareTo(MyKeyValuePair<E> o) {
                if (key == null) {
                    return -1;
                } else if (o.key == null) {
                    return -1;
                } else {
                    return String.valueOf(key).compareTo(String.valueOf(o.key));
                }
            }
        }
    }
}
