package org.mymarketapp.reactive.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("item_id")
    private Long itemId;

    private String title;
    private Long price;
    private Integer count;

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getItemId() { return itemId; }
    public String getTitle() { return title; }
    public Long getPrice() { return price; }
    public Integer getCount() { return count; }

    public void setId(Long id) { this.id = id; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public void setTitle(String title) { this.title = title; }
    public void setPrice(Long price) { this.price = price; }
    public void setCount(Integer count) { this.count = count; }
}
