package org.mymarketapp.reactive.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("cart_items")
public class CartItem implements Persistable<Long> {

    @Id
    @Column("item_id")
    private Long itemId;

    private Integer count;

    @Transient
    private boolean newEntity;

    public CartItem() {
    }

    public CartItem(Long itemId, Integer count) {
        this.itemId = itemId;
        this.count = count;
        this.newEntity = true;
    }

    @Override
    public Long getId() {
        return itemId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public Long getItemId() {
        return itemId;
    }

    public Integer getCount() {
        return count;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
