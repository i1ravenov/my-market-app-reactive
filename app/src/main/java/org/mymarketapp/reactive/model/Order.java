package org.mymarketapp.reactive.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("orders")
public class Order {

    @Id
    private Long id;

    @Column("total_sum")
    private Long totalSum;

    public Long getId() { return id; }
    public Long getTotalSum() { return totalSum; }

    public void setId(Long id) { this.id = id; }
    public void setTotalSum(Long totalSum) { this.totalSum = totalSum; }
}
