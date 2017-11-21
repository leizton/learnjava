package com.whiker.learn.easymock.test;

import com.whiker.learn.easymock.Goods;
import com.whiker.learn.easymock.GoodsDao;
import com.whiker.learn.easymock.ShoppingCart;
import com.whiker.learn.easymock.ShoppingItem;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * @author whiker@163.com create on 16-5-22.
 */
public class ShoppingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingTest.class);

    private GoodsDao goodsDao;

    @Before
    public void initGoodsDao() {
        goodsDao = EasyMock.createMock(GoodsDao.class);
        goodsDao.queryById(1);
        EasyMock.expectLastCall().andReturn(new Goods(1, "牛奶", new BigDecimal("5.99")));
        goodsDao.queryById(2);
        EasyMock.expectLastCall().andReturn(new Goods(2, "面包", new BigDecimal("3.99")));
        EasyMock.replay(goodsDao);
    }

    @Test
    public void test() {
        ShoppingCart cart = new ShoppingCart();

        ShoppingItem item1 = new ShoppingItem(goodsDao.queryById(1), 2);
        LOGGER.info("购物项: {}, {}, {}", new Object[]{
                item1.getGoods().getName(), item1.getQuantity(), item1.getPrice()});

        ShoppingItem item2 = new ShoppingItem(goodsDao.queryById(2), 3);
        LOGGER.info("购物项: {}, {}, {}", new Object[]{
                item2.getGoods().getName(), item2.getQuantity(), item2.getPrice()});

        cart.addOrModifyItem(item1);
        cart.addOrModifyItem(item2);
        LOGGER.info("购物车总价: {}", cart.getPrice());
    }
}
