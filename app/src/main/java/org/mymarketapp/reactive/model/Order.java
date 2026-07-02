package org.mymarketapp.reactive.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("orders")
@Getter
@Setter
public class Order {

    @Id
    private Long id;

    @Column("total_sum")
    private Long totalSum;
}
