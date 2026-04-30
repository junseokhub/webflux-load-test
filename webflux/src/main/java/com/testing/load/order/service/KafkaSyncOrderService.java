package com.testing.load.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.order.domain.Order;
import com.testing.load.order.dto.OrderMessage;
import com.testing.load.order.dto.OrderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KafkaSyncOrderService implements OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, MonoSink<Order>> pendingOrders = new ConcurrentHashMap<>();

    public KafkaSyncOrderService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Order> createOrder(Long userId, Long productId, Long couponIssueId) {
        return Mono.<Order>create(sink -> {
            try {
                String correlationId = UUID.randomUUID().toString();
                pendingOrders.put(correlationId, sink);
                OrderMessage message = new OrderMessage(userId, productId, couponIssueId, correlationId);
                kafkaTemplate.send("order-requests", correlationId,
                        objectMapper.writeValueAsString(message));
            } catch (Exception e) {
                sink.error(e);
            }
        }).timeout(Duration.ofSeconds(10));
    }

    @KafkaListener(topics = "order-results", groupId = "order-sync-group")
    public void onOrderResult(String message) {
        try {
            OrderResult result = objectMapper.readValue(message, OrderResult.class);
            MonoSink<Order> sink = pendingOrders.remove(result.correlationId());
            if (sink == null) return;

            if (result.success()) {
                Order order = Order.builder()
                        .userId(result.userId())
                        .productId(result.productId())
                        .couponIssueId(result.couponIssueId())
                        .originalPrice(result.originalPrice())
                        .finalPrice(result.finalPrice())
                        .build();
                sink.success(order);
            } else {
                sink.error(new RuntimeException(result.errorMessage()));
            }
        } catch (Exception e) {
            log.error("order-results 처리 실패: {}", e.getMessage());
        }
    }
}