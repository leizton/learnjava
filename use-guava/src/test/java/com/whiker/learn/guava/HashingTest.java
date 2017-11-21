package com.whiker.learn.guava;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by whiker on 16-4-17.
 * 使用guava的MD5.
 */
public class HashingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HashingTest.class);

    @Test
    public void testMD5() {
        Assert.assertEquals(
                getMd5WithKey("key-1", "中文message"),
                getMd5NoKey("key-1中文message"));
        Assert.assertEquals(
                getMd5WithKey("密钥", "中文message"),
                getMd5NoKey("密钥中文message"));
    }

    @Test
    public void testCRC32() {
        getCrc32("message");
        getCrc32("中文message");
    }

    /**
     * 有key的MD5, 实际上就是两个字符串连接后得到的新字符串作MD5.
     * Hashing.md5()
     *         .newHasher()
     *         .putString(key, Charsets.UTF_8)
     *         .putString(message, Charsets.UTF_8)
     *         .hash().toString()
    */
    private String getMd5WithKey(String key, String message) {
        checkArgument(!Strings.isNullOrEmpty(message), "getMd5(), message是null或空串.");
        Hasher hasherSrc = Hashing.md5().newHasher();
        Hasher hasherKey = hasherSrc.putString(key, Charsets.UTF_8);
        Hasher hasher = hasherKey.putString(message, Charsets.UTF_8);
        HashCode hashCode = hasher.hash();
        return hashCodeToString(key + ", " + message, "MD5", hashCode);
    }

    /**
     * 没有key的MD5
     * Hashing.md5()
     *         .hashString(message, Charsets.UTF_8)
     *         .toString()
     */
    private String getMd5NoKey(String message) {
        checkArgument(!Strings.isNullOrEmpty(message), "getMd5(), message是null或空串.");
        HashCode hashCode = Hashing.md5().hashString(message, Charsets.UTF_8);
        return hashCodeToString(message, "MD5", hashCode);
    }

    /**
     * 没有key的CRC32
     */
    private String getCrc32(String message) {
        checkArgument(!Strings.isNullOrEmpty(message), "getCrc32(), message是null或空串.");
        HashCode hashCode = Hashing.crc32().hashString(message, Charsets.UTF_8);
        return hashCodeToString(message, "CRC32", hashCode);
    }

    private String hashCodeToString(String message, String algorithm, HashCode hashCode) {
        checkNotNull(hashCode, "hashCodeToString(), hashCode是null.");
        String str = hashCode.toString();
        LOGGER.info("{}({}) = {}", new Object[]{algorithm, message, str});
        return str;
    }
}
