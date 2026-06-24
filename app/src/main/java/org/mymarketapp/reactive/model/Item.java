package org.mymarketapp.reactive.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("items")
public class Item {

    @Id
    private Long id;
    private String title;
    private String description;

    @Column("img_path")
    private String imgPath;

    private Long price;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImgPath() { return imgPath; }
    public Long getPrice() { return price; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setImgPath(String imgPath) { this.imgPath = imgPath; }
    public void setPrice(Long price) { this.price = price; }
}
