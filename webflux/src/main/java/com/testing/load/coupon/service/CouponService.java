package com.testing.load.coupon.service;

import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.coupon.domain.Coupon;
import com.testing.load.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Transactional
    public Mono<Coupon> createCoupon(String name, String type, int discountValue, int totalStock) {
        return couponRepository.save(
                Coupon.builder()
                        .name(name)
                        .type(type)
                        .discountValue(discountValue)
                        .totalStock(totalStock)
                        .build()
        ).flatMap(coupon ->
                reactiveRedisTemplate.opsForValue()
                        .set(stockKey(coupon.getId()), String.valueOf(totalStock))
                        .thenReturn(coupon)
        );
    }

    @Transactional(readOnly = true)
    public Flux<Coupon> findAll() {
        return couponRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Mono<Coupon> findById(Long couponId) {
        return couponRepository.findById(couponId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.COUPON_NOT_FOUND)));

    }

    private String stockKey(Long couponId) {
        return "{coupon:" + couponId + "}:stock";
    }
}
