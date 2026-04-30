package com.testing.load.order.service;

import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.coupon.repository.CouponIssueRepository;
import com.testing.load.coupon.repository.CouponRepository;
import com.testing.load.order.domain.Order;
import com.testing.load.order.repository.OrderRepository;
import com.testing.load.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultOrderService {

    private final OrderRepository orderRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final CouponRepository couponRepository;

    public Mono<Order> saveOrder(Long userId, Product product, Long couponIssueId) {
        return resolveFinalPrice(product, couponIssueId)
                .flatMap(finalPrice -> orderRepository.save(
                        Order.builder()
                                .userId(userId)
                                .productId(product.getId())
                                .couponIssueId(couponIssueId)
                                .originalPrice(product.getPrice())
                                .finalPrice(finalPrice)
                                .build()
                ));
    }

    private Mono<Integer> resolveFinalPrice(Product product, Long couponIssueId) {
        if (couponIssueId == null) {
            return Mono.just(product.getPrice());
        }

        return couponIssueRepository.findById(couponIssueId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.COUPON_ISSUE_NOT_FOUND)))
                .flatMap(couponIssue -> couponRepository.findById(couponIssue.getCouponId())
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.COUPON_NOT_FOUND)))
                        .map(coupon -> calculatePrice(
                                product.getPrice(),
                                coupon.getType(),
                                coupon.getDiscountValue()
                        ))
                );
    }

    private int calculatePrice(int price, String type, int discountValue) {
        return switch (type) {
            case "PERCENT" -> price - (price * discountValue / 100);
            case "FIXED" -> Math.max(0, price - discountValue);
            case "FREE" -> 0;
            default -> price;
        };
    }
}