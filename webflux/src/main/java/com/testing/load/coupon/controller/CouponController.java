package com.testing.load.coupon.controller;

import com.testing.load.coupon.dto.CouponIssueResponse;
import com.testing.load.coupon.dto.CouponRequest;
import com.testing.load.coupon.dto.CouponResponse;
import com.testing.load.coupon.service.CouponIssueService;
import com.testing.load.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CouponIssueService couponIssueService;

    // 쿠폰 생성(ADMIN전용)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CouponResponse> createCoupon(@RequestBody CouponRequest request) {
        return couponService.createCoupon(
                request.name(),
                request.type(),
                request.discountValue(),
                request.totalStock()
        ).map(CouponResponse::from);
    }

    // 쿠폰 조회
    @GetMapping("/{couponId}")
    public Mono<CouponResponse> findById(@PathVariable Long couponId) {
        return couponService.findById(couponId)
                .map(CouponResponse::from);
    }

    @GetMapping
    public Flux<CouponResponse> findAll() {
        return couponService.findAll()
                .map(CouponResponse::from);
    }

    // 쿠폰 발급 (USER)
    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CouponIssueResponse> issueCoupon(@PathVariable Long couponId, @AuthenticationPrincipal Mono<Long> userId) {
        return userId.flatMap(id -> couponIssueService.issueCoupon(couponId, id))
                .map(CouponIssueResponse::from);
    }
}
