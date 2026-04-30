package com.testing.load.coupon.repository;

import com.testing.load.coupon.domain.CouponIssue;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CouponIssueRepository extends ReactiveCrudRepository<CouponIssue, Long> {
    Mono<Boolean> existsByCouponIdAndUserId(Long couponId, Long userId);
}
