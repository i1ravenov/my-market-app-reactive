package org.mymarketapp.reactive.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("items")
@Getter
@Setter
public class Item {

    @Id
    private Long id;
    private String title;
    private String description;

    @Column("img_path")
    private String imgPath;

    private Long price;
}
