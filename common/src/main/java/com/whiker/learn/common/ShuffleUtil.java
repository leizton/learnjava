package com.whiker.learn.common;

import java.util.Random;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 洗牌, 时间 O(n), 空间 O(1)
 * Created by whiker on 16-4-4.
 */
public class ShuffleUtil {

    /**
     * 洗牌
     *
     * @param cards 待洗牌的数组
     * @param <T>   数组元素类型
     */
    public static <T> void shuffle(T[] cards) {
        checkNotNull(cards);

        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());

        for (int i = 1; i < cards.length; i++) {
            int j = rand.nextInt(i + 1);  // 只跟自己或前面的元素交换
            if (j != i) {
                T temp = cards[i];
                cards[i] = cards[j];
                cards[j] = temp;
            }
        }
    }
}
