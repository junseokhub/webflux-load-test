package com.testing.load.order.service;

import com.testing.load.order.domain.Order;
import reactor.core.publisher.Mono;

public interface OrderService {
    Mono<Order> createOrder(Long userId, Long productId, Long couponIssueId);
}