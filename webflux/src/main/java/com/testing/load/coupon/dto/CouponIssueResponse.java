package com.testing.load.coupon.dto;

import com.testing.load.coupon.domain.CouponIssue;

import java.time.LocalDateTime;

public record CouponIssueResponse(
        Long id,
        Long couponId,
        Long userId,
        LocalDateTime createdAt
) {

    public static CouponIssueResponse from(CouponIssue couponIssue) {
        return new CouponIssueResponse(
                couponIssue.getId(),
                couponIssue.getCouponId(),
                couponIssue.getUserId(),
                couponIssue.getCreatedAt()
        );
    }
}
