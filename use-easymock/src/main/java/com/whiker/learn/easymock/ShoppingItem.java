package com.whiker.learn.easymock;

import java.math.BigDecimal;

/**
 * @author whiker@163.com create on 16-5-22.
 */
public class ShoppingItem {
    private final Goods goods;
    private int quantity;

    public ShoppingItem(Goods goods, int quantity) {
        this.goods = goods;
        this.quantity = quantity <= 0 ? 0 : quantity;
    }

    public Goods getGoods() {
        return goods;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * 返回该购物项的价格
     */
    public BigDecimal getPrice() {
        if (goods == null || goods.getPrice() == null || quantity <= 0) {
            return new BigDecimal(0);
        }
        return goods.getPrice().multiply(new BigDecimal(quantity));
    }
}
