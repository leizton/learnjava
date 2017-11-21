package com.whiker.learn.easymock;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author whiker@163.com create on 16-5-22.
 *         非线程安全
 */
public class ShoppingCart {
    /**
     * Goods.id到ShoppingItem的映射
     *
     * @see com.whiker.learn.easymock.Goods
     * @see com.whiker.learn.easymock.ShoppingItem
     */
    private Map<Integer, ShoppingItem> items = Maps.newHashMap();

    /**
     * 添加新购物项, 修改购买数量, 删除购买项
     *
     * @throws IllegalArgumentException
     */
    public void addOrModifyItem(ShoppingItem item) {
        Preconditions.checkArgument(item != null, "参数item是null");
        Preconditions.checkArgument(item.getGoods() != null, "item的商品是null");
        Preconditions.checkArgument(item.getQuantity() >= 0, "item的数量非法");

        ShoppingItem oldItem = items.get(item.getGoods().getId());
        if (oldItem == null) { // 添加新购物项
            items.put(item.getGoods().getId(), item);
        } else {
            if (item.getQuantity() > 0) { // 修改购买数量
                oldItem.setQuantity(item.getQuantity());
            } else { // 删除购买项
                items.remove(item.getGoods().getId());
            }
        }
    }

    /**
     * 计算购物车总价格
     */
    public BigDecimal getPrice() {
        BigDecimal totalPrice = new BigDecimal(0);
        for (ShoppingItem item : items.values()) {
            totalPrice = totalPrice.add(item.getPrice());
        }
        return totalPrice;
    }
}
