package com.mvc.load.product;

import com.mvc.load.common.entity.BaseEntity;
import com.mvc.load.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "products")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Integer price;

    @Column()
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Builder
    public Product(String name, Integer price, Integer stock, User createdBy) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.createdBy = createdBy;
    }

    public void decrementStock() {
        this.stock--;
    }
}
