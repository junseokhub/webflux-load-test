package com.testing.load.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.order.dto.OrderMessage;
import com.testing.load.order.dto.OrderResult;
import com.testing.load.order.service.RedisLuaOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
@RequiredArgsConstructor
public class LuaOrderKafkaConsumer implements OrderKafkaConsumer {

    private final RedisLuaOrderService redisLuaOrderService;
    private final KafkaConsumerSupport support;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-requests", groupId = "order-consumer-group")
    public void consume(String message) {
        try {
            OrderMessage orderMessage = objectMapper.readValue(message, OrderMessage.class);
            processOrder(orderMessage);
        } catch (Exception e) {
            log.error("order-requests 처리 실패, DLQ로 이동: {}", e.getMessage());
            support.sendToDlq(message);
        }
    }

    private void processOrder(OrderMessage orderMessage) {
        redisLuaOrderService.createOrder(
                        orderMessage.userId(),
                        orderMessage.productId(),
                        orderMessage.couponIssueId()
                )
                .doOnSuccess(order -> support.sendResult(OrderResult.success(
                        orderMessage.correlationId(), order)))
                .doOnError(e -> support.handleError(e, orderMessage))
                .subscribe();
    }
}