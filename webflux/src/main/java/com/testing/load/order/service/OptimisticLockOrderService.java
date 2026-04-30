package com.testing.load.order.service;

import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.order.domain.Order;
import com.testing.load.product.domain.Product;
import com.testing.load.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@RequiredArgsConstructor
public class OptimisticLockOrderService implements OrderService {

    private static final int MAX_RETRY = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(50);

    private final ProductRepository productRepository;
    private final DefaultOrderService defaultOrderService;

    @Override
    @Transactional
    public Mono<Order> createOrder(Long userId, Long productId, Long couponIssueId) {
        return decrementStock(productId)
                .flatMap(product -> defaultOrderService.saveOrder(userId, product, couponIssueId));
    }

    private Mono<Product> decrementStock(Long productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)))
                .flatMap(product -> {
                    if (product.getStock() <= 0) {
                        return Mono.error(new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK));
                    }
                    product.decrementStock();
                    return productRepository.save(product);
                })
                .retryWhen(Retry.backoff(MAX_RETRY, RETRY_DELAY)
                        .filter(e -> e instanceof OptimisticLockingFailureException)
                        .onRetryExhaustedThrow((spec, signal) ->
                                new BusinessException(ErrorCode.PRODUCT_ORDER_FAILED)));
    }
}