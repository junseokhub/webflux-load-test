package com.testing.load.order.service;

import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.order.domain.Order;
import com.testing.load.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisLuaOrderService implements OrderService {

    private final ProductRepository productRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final DefaultOrderService defaultOrderService;

    private static final String DECREMENT_STOCK_SCRIPT = """
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil or stock <= 0 then
                return -1
            end
            redis.call('DECR', KEYS[1])
            return 1
            """;

    @Override
    public Mono<Order> createOrder(Long userId, Long productId, Long couponIssueId) {
        String stockKey = "{product:" + productId + "}:stock";

        return reactiveRedisTemplate.execute(
                        RedisScript.of(DECREMENT_STOCK_SCRIPT, Long.class),
                        List.of(stockKey)
                )
                .next()
                .flatMap(result -> {
                    if (result == -1L) {
                        return Mono.error(new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK));
                    }
                    return productRepository.findById(productId)
                            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));
                })
                .flatMap(product -> defaultOrderService.saveOrder(userId, product, couponIssueId));
    }
}