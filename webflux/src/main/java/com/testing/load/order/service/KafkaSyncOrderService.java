package com.testing.load.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.order.domain.Order;
import com.testing.load.order.dto.OrderMessage;
import com.testing.load.order.dto.OrderResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class KafkaSyncOrderService implements OrderService {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    private final Map<String, MonoSink<Order>> pendingOrders = new ConcurrentHashMap<>();

    @Override
    public Mono<Order> createOrder(Long userId, Long productId, Long couponIssueId, String correlationId) {
        // KAFKA_SYNC도 자체 생성. 외부 correlationId는 보통 null로 들어옴.
        String effectiveCorrelationId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        return Mono.<Order>create(sink -> {
                    pendingOrders.put(effectiveCorrelationId, sink);

                    try {
                        OrderMessage message = new OrderMessage(userId, productId, couponIssueId, effectiveCorrelationId);
                        String payload = objectMapper.writeValueAsString(message);

                        SenderRecord<String, String, String> record = SenderRecord.create(
                                new ProducerRecord<>("order-requests", effectiveCorrelationId, payload),
                                effectiveCorrelationId
                        );

                        kafkaSender.send(Mono.just(record))
                                .next()
                                .doOnError(e -> {
                                    pendingOrders.remove(effectiveCorrelationId);
                                    sink.error(e);
                                })
                                .subscribe();
                    } catch (Exception e) {
                        pendingOrders.remove(effectiveCorrelationId);
                        sink.error(e);
                    }
                })
                .timeout(Duration.ofSeconds(10))
                .doFinally(signal -> pendingOrders.remove(effectiveCorrelationId));
    }

    private void handleResult(String message) {
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