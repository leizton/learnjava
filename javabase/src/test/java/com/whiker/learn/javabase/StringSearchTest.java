package com.whiker.learn.javabase;

import com.google.common.base.Strings;
import com.whiker.learn.common.CostTimer;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author whiker@163.com create on 16-6-18.
 */
public class StringSearchTest {

    private static final String SEARCH_STR = "abcdefghijklmnopqrstuvwxyz"; // 查找字符串
    private static final Pattern SEARCH_REGEX = Pattern.compile(SEARCH_STR); // 查找所用的正则模式

    /**
     * 字符串匹配查找的性能比较
     * <p>
     * String.indexOf()是朴素查找, 时间复杂度O(m(n-m+1)), 测试用时 6155ms
     * 正则是DFA, 时间复杂度O(n), 测试用时 1846ms 和 1783ms
     * KMP算法时间复杂度O(n)
     */
    @Test
    public void testSearch() {
        String source = constructSpecialSourceStr(SEARCH_STR, '0', 10000);
        CostTimer time = new CostTimer();
        int runNum = 10000; // 运行次数;
        long indexofTime, regexedTime, regexingTime;

        // 预热
        for (int i = 0; i < 1000; i++) {
            source.indexOf(SEARCH_STR);
        }

        // indexOf
        time.start();
        for (int i = 0; i < runNum; i++) {
            source.indexOf(SEARCH_STR);
        }
        indexofTime = time.end();

        // 已编译好的正则
        time.start();
        for (int i = 0; i < runNum; i++) {
            SEARCH_REGEX.split(source);
        }
        regexedTime = time.end();

        // 临时编译正则
        time.start();
        for (int i = 0; i < runNum; i++) {
            Pattern.compile(SEARCH_STR).split(source);
        }
        regexingTime = time.end();

        System.out.println("indexOf用时: " + indexofTime);
        System.out.println("已编译好的正则用时: " + regexedTime);
        System.out.println("临时编译正则用时: " + regexingTime);
    }

    /**
     * 用查找字符串构造特殊的待查找原字符串
     *
     * @param search    查找字符串
     * @param confuseCh 混淆searchStr的字符
     * @param repeatNum 控制复杂度
     * @return 待查找原字符串
     */
    private String constructSpecialSourceStr(String search, char confuseCh, int repeatNum) {
        checkArgument(search != null && search.length() > 1, "search参数非法");
        checkArgument(repeatNum > 1, "repeatNum参数非法");

        // confuseStr只有最后一个字符和searchStr不同
        String confuseStr = search.substring(0, search.length() - 1) + confuseCh;

        return Strings.repeat(confuseStr, repeatNum) + search;
    }
}
