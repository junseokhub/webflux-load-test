package com.testing.load.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.order.domain.Order;
import com.testing.load.order.dto.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class KafkaAsyncOrderService implements OrderService {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Order> createOrder(Long userId, Long productId, Long couponIssueId, String correlationId) {
        // KAFKA_ASYNC 모드는 자체적으로 correlationId를 생성.
        // 외부에서 받은 correlationId는 무시하거나, null이면 새로 생성.
        String effectiveCorrelationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        OrderMessage message = new OrderMessage(userId, productId, couponIssueId, effectiveCorrelationId);
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(message))
                .flatMap(payload -> {
                    SenderRecord<String, String, String> record = SenderRecord.create(
                            new ProducerRecord<>("order-requests", effectiveCorrelationId, payload),
                            effectiveCorrelationId
                    );
                    return kafkaSender.send(Mono.just(record))
                            .next()
                            .doOnError(e -> log.error("Kafka 발행 실패: {}", e.getMessage()))
                            .thenReturn(Order.pending(userId, productId, couponIssueId));
                });
    }
}