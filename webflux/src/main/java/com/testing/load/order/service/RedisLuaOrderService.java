package com.testing.load.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.order.domain.Order;
import com.testing.load.outbox.domain.Outbox;
import com.testing.load.outbox.repository.OutboxRepository;
import com.testing.load.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisLuaOrderService implements OrderService {

    private final ProductRepository productRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final DefaultOrderService defaultOrderService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final String DECREMENT_STOCK_SCRIPT = """
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil or stock <= 0 then
                return -1
            end
            redis.call('DECR', KEYS[1])
            return 1
            """;

    @Override
    @Transactional
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
                    // Redis stock 차감 완료 — 이후 DB 실패 시 반드시 보상 필요
                    return saveOrderWithOutbox(userId, productId, couponIssueId)
                            .onErrorResume(e -> compensateRedisStock(stockKey).then(Mono.error(e)));
                });
    }

    private Mono<Order> saveOrderWithOutbox(Long userId, Long productId, Long couponIssueId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)))
                .flatMap(product -> defaultOrderService.saveOrder(userId, product, couponIssueId))
                .flatMap(order -> {
                    try {
                        String payload = objectMapper.writeValueAsString(
                                new OutboxPayload(productId, order.getId())
                        );
                        return outboxRepository.save(
                                Outbox.builder()
                                        .aggregateId(productId)
                                        .eventType("STOCK_DECREMENT")
                                        .payload(payload)
                                        .build()
                        ).thenReturn(order);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    private Mono<Void> compensateRedisStock(String stockKey) {
        return reactiveRedisTemplate.opsForValue().increment(stockKey).then();
    }

    public record OutboxPayload(Long productId, Long orderId) {}
}