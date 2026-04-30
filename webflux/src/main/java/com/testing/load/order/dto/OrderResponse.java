package com.testing.load.order.dto;

import com.testing.load.order.domain.Order;
import com.testing.load.order.domain.OrderStatus;

public record OrderResponse(
        Long id,
        Long userId,
        Long productId,
        Long couponIssueId,
        int originalPrice,
        int finalPrice,
        OrderStatus status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getCouponIssueId(),
                order.getOriginalPrice(),
                order.getFinalPrice(),
                order.getStatus()
        );
    }
}