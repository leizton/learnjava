package com.whiker.learn.common;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author whiker@163.com create on 16-6-19.
 */
public class Strings {

    /**
     * 在source中找到search第一次出现的位置
     * 若source中没有search则返回-1
     *
     * @throws IllegalArgumentException
     */
    public static int indexOf(String source, String search) {
        return indexOf(source, search, null, 0);
    }

    /**
     * 可设置查找起始位置start
     *
     * @throws IllegalArgumentException
     */
    public static int indexOf(String source, String search, int start) {
        return indexOf(source, search, null, start);
    }

    /**
     * 当使用固定的search查找不同的source时, 可复用前缀函数prefixFunction
     * 前缀函数可通过Strings#prefixFunction获得
     *
     * @throws IllegalArgumentException
     * @see Strings#prefixFunction
     */
    public static int indexOf(String source, String search, int[] prefixFunction) {
        return indexOf(source, search, prefixFunction, 0);
    }

    /**
     * 从source的第start个字符开始查找search第一次出现的位置
     * 若没有找到search则返回-1
     * 使用KMP算法实现, 时间O(source.length), 空间O(search.length)
     *
     * @throws IllegalArgumentException
     */
    public static int indexOf(String source, String search, int[] pf, int start) {
        checkArgument(source != null, "source == null");
        checkArgument(search != null, "search == null");
        checkArgument(pf == null || pf.length == search.length(), "prefixFunction's length != search's length");
        checkArgument(start >= 0, "start < 0");

        if (start >= source.length() || search.length() > source.length()) {
            return -1;
        }
        final int n = source.length();
        final int m = search.length();
        pf = pf == null ? prefixFunction(search) : pf;

        int i = start, j = 0;
        while (i < n && j < m) {
            if (source.charAt(i) != search.charAt(j)) {
                if (j > 0) {
                    /**
                     * 移动距离 = 已匹配长度 - 最后一个匹配成功字符search[j-1]的前缀函数
                     * 例如: source="abac..." search="ababc" pf=[0 1 2 3 0]
                     *   a b a |c| ...  [i:3] ==>  a b a |c| ...    [i:3]
                     *   a b a |b| a c  [j:3] ==>      a |b| a b c  [j:1]
                     */
                    j = pf[j - 1] == 0 ? 0 : (j - pf[j - 1]);  // 若pf[j-1]==0, 则从头匹配
                } else {
                    i++;
                }
            } else {
                i++;
                j++;
            }
        }
        return j == m ? (i - m) : -1;
    }

    /**
     * 计算字符串s的前缀函数
     */
    public static int[] prefixFunction(String s) {
        checkArgument(s != null, "argument \"s\" is null");
        if (s.length() == 0) {
            return new int[0];
        }
        int n = s.length();
        int[] pf = new int[n];
        pf[0] = 0;
        int k = 0;

        /**
         * 状态变量k, 是{以s[i-1]结尾的最长后缀}所对应的最长前缀的长度, 也即pf[i-1]
         */
        for (int i = 1; i < n; i++) {
            while (k > 0 && s.charAt(k) != s.charAt(i)) {  // s[i-1] == s[k-1]
                k = pf[k - 1];  // s[k-1] == s[pf[k-1] - 1] == s[i-1]
            }
            if (s.charAt(i) == s.charAt(k)) {
                k++;
            }
            pf[i] = k;
        }
        return pf;
    }
}
