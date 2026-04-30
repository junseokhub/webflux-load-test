package com.testing.load.coupon.dto;

public record CouponRequest(
        String name,
        String type,
        int discountValue,
        int totalStock
) {}