package com.testing.load.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.order.domain.Order;
import com.testing.load.order.dto.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
public class KafkaAsyncOrderService implements OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaAsyncOrderService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Order> createOrder(Long userId, Long productId, Long couponIssueId) {
        return Mono.fromCallable(() -> {
            String correlationId = UUID.randomUUID().toString();
            OrderMessage message = new OrderMessage(userId, productId, couponIssueId, correlationId);
            kafkaTemplate.send("order-requests", correlationId,
                    objectMapper.writeValueAsString(message));
            return Order.pending(userId, productId, couponIssueId);
        });
    }
}