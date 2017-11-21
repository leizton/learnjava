package com.whiker.learn.guava;

import com.google.common.base.Splitter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by whiker on 16-3-20.
 * 使用guava的Splitter
 */
public class SplitterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SplitterTest.class);

    private static final Splitter SPACE_SPLITTER =
            Splitter.on(' ').trimResults().omitEmptyStrings();

    @Test
    public void testOn() {
        String s = "a b \tc\t \t d";
        List<String> strs = SPACE_SPLITTER.splitToList(s);
        Assert.assertArrayEquals(
                new String[]{"a", "b", "c", "d"},
                strs.toArray());
        LOGGER.info("{}", strs);
    }
}
