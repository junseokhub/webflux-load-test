package com.mvc.load.order.service;

import com.mvc.load.order.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import com.mvc.load.order.consumer.OrderMessage;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class KafkaOrderService implements OrderService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Order createOrder(Long userId, Long productId) {
        try {
            String correlationId = UUID.randomUUID().toString();
            OrderMessage message = new OrderMessage(userId, productId, correlationId);
            kafkaTemplate.send("order-requests", correlationId,
                    objectMapper.writeValueAsString(message));
            return Order.pending(userId, productId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}