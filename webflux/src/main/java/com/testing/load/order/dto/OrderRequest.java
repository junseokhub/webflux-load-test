package com.testing.load.order.dto;

public record OrderRequest(
        Long productId,
        Long couponIssueId
) {}