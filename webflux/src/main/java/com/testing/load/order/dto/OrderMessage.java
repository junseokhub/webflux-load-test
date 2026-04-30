package com.testing.load.order.dto;

public record OrderMessage(
        Long userId,
        Long productId,
        Long couponIssueId,
        String correlationId
) {}