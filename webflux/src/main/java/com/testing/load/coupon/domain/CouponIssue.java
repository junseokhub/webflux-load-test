package com.testing.load.coupon.domain;

import com.testing.load.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Getter
@NoArgsConstructor
@Table("coupon_issues")
public class CouponIssue extends BaseEntity {

    @Id
    private Long id;
    private Long couponId;
    private Long userId;

    @Builder
    public CouponIssue(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
    }
}