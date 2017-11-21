package com.whiker.learn.easymock;

import java.math.BigDecimal;

/**
 * @author whiker@163.com create on 16-5-22.
 *         商品Bean
 */
public class Goods {
    private final int id;
    private final String name; // 名称
    private final BigDecimal price; // 价格

    public Goods(int id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
