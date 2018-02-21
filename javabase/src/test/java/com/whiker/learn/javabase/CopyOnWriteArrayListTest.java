package com.whiker.learn.javabase;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 2018/2/20
 */
public class CopyOnWriteArrayListTest {

    public static void main(String[] args) {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 6; i++) {
            list.add(String.valueOf(i));
        }

        System.out.print("\n>> ");
        for (String i : list) {
            list.remove("3");
            list.remove("4");
            System.out.print(i + " ");
        }

        System.out.print("\n>> ");
        for (String i : list) {
            System.out.print(i + " ");
        }
    }
}
