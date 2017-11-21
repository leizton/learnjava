package com.whiker.learn.common.test;

import com.whiker.learn.common.Strings;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author whiker@163.com create on 16-6-19.
 */
public class StringsTest {

    @Test
    public void testPrefixFunction() {
        String s = "ababac";
        Assert.assertArrayEquals(new int[]{0, 0, 1, 2, 3, 0}, Strings.prefixFunction(s));
        s = "aaaaaa";
        Assert.assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5}, Strings.prefixFunction(s));
        s = "abcdef";
        Assert.assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0}, Strings.prefixFunction(s));
        s = "";
        Assert.assertArrayEquals(new int[]{}, Strings.prefixFunction(s));

        try {
            Strings.prefixFunction(null);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    @Test
    public void testIndexOf() {
        testIndexOf("abcd", "", 0, 0);
        testIndexOf("abcd", "", 2, 2);
        testIndexOf("", "ababc", 0, -1);

        testIndexOf("abacababc", "ababc", 0, 4);
        testIndexOf("abacababc", "ababc", 4, 4);
        testIndexOf("abacababc", "ababc", 5, -1);
        testIndexOf("abacababc", "ababc", 100, -1);

        testIndexOf("cbacababc", "ababc", 0, 4);
        testIndexOf("abaaababc", "ababc", 0, 4);
        testIndexOf("abaababc", "ababc", 0, 3);
        testIndexOf("ababcababcb", "ababcb", 0, 5);
        testIndexOf("abcdeabcdeabcdz", "abcdz", 0, 10);
        testIndexOf("abcdeabcdeabcdz", "abcdl", 0, -1);

        String search = "ababc";
        int[] pf = Strings.prefixFunction(search);
        Assert.assertEquals(4, Strings.indexOf("cbacababc", search, pf, 0));
        Assert.assertEquals(5, Strings.indexOf("abaadababc", search, pf, 0));
    }

    private void testIndexOf(String source, String search, int start, int expectResult) {
        Assert.assertEquals(source.indexOf(search, start),
                Strings.indexOf(source, search, start));
    }
}
