package com.testing.load.order.controller;

import com.testing.load.common.response.ApiResponse;
import com.testing.load.order.dto.OrderRequest;
import com.testing.load.order.dto.OrderResponse;
import com.testing.load.order.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostConstruct
    public void init() {
        log.info(">>> Controller에 주입된 OrderService: {}", orderService.getClass().getSimpleName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<OrderResponse>> createOrder(
            @RequestBody OrderRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return orderService.createOrder(
                userId,
                request.productId(),
                request.couponIssueId(),
                null
        ).map(OrderResponse::from).map(ApiResponse::ok);
    }
}