package com.testing.load.coupon.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Table("coupons")
public class Coupon {

    @Id
    private Long id;
    private String name;
    private String type;        // PERCENT, FIXED, FREE
    private int discountValue;  // PERCENT: 10(%), FIXED: 5000(원), FREE: 0
    private int totalStock;
    private int remainingStock;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Coupon(String name, String type, int discountValue, int totalStock) {
        this.name = name;
        this.type = type;
        this.discountValue = discountValue;
        this.totalStock = totalStock;
        this.remainingStock = totalStock;
    }
}