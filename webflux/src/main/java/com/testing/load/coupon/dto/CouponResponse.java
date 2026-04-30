package com.testing.load.coupon.dto;

import com.testing.load.coupon.domain.Coupon;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String name,
        String type,
        int discountValue,
        int totalStock,
        int remainingStock,
        LocalDateTime createdAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getType(),
                coupon.getDiscountValue(),
                coupon.getTotalStock(),
                coupon.getRemainingStock(),
                coupon.getCreatedAt()
        );
    }
}