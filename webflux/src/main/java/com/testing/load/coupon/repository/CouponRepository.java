package com.testing.load.coupon.repository;

import com.testing.load.coupon.domain.Coupon;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CouponRepository extends ReactiveCrudRepository<Coupon, Long> {
}
