package com.whiker.learn.easymock;

/**
 * @author whiker@163.com create on 16-5-22.
 */
public interface GoodsDao {
    /**
     * 根据商品id获取商品bean
     */
    Goods queryById(int goodsId);
}
