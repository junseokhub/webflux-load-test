package com.testing.load.order.dto;

import com.testing.load.order.domain.Order;

public record OrderResult(
        String correlationId,
        Long orderId,
        Long userId,
        Long productId,
        Long couponIssueId,
        int originalPrice,
        int finalPrice,
        boolean success,
        String errorMessage
) {
    public static OrderResult success(String correlationId, Order order) {
        return new OrderResult(
                correlationId,
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getCouponIssueId(),
                order.getOriginalPrice(),
                order.getFinalPrice(),
                true,
                null
        );
    }

    public static OrderResult failure(String correlationId, String errorMessage) {
        return new OrderResult(
                correlationId,
                null, null, null, null, 0, 0,
                false,
                errorMessage
        );
    }
}