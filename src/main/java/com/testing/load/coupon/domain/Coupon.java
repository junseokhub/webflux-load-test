package com.testing.load.coupon.domain;

import com.testing.load.common.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@NoArgsConstructor
@Table("coupons")
public class Coupon extends BaseEntity {

    @Id
    private Long id;
    private String name;
    private int totalStock;
    private int remainingStock;

    public Coupon(String name, int totalStock) {
        this.name = name;
        this.totalStock = totalStock;
        this.remainingStock = totalStock;
    }
}