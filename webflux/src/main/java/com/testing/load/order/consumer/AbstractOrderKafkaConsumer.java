package com.testing.load.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.order.dto.OrderMessage;
import com.testing.load.order.dto.OrderResult;
import com.testing.load.order.domain.Order;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOrderKafkaConsumer implements OrderKafkaConsumer {

    private static final int CONCURRENCY = 32;

    protected final KafkaConsumerSupport support;
    protected final ObjectMapper objectMapper;
    protected final ReceiverOptions<String, String> receiverOptions;

    private Disposable subscription;

    @PostConstruct
    public void start() {
        subscription = KafkaReceiver.create(receiverOptions)
                .receive()
                .flatMap(this::processRecord, CONCURRENCY)
                .subscribe();
        log.info("{} 시작", getClass().getSimpleName());
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("{} 종료", getClass().getSimpleName());
        }
    }

    private Mono<Void> processRecord(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> objectMapper.readValue(record.value(), OrderMessage.class))
                .flatMap(orderMessage ->
                        createOrder(orderMessage)
                                .doOnSuccess(order -> support.sendResult(
                                        OrderResult.success(orderMessage.correlationId(), order)))
                                .onErrorResume(e -> {
                                    support.handleError(e, orderMessage);
                                    return Mono.empty();
                                })
                )
                .doFinally(signal -> record.receiverOffset().acknowledge())
                .onErrorResume(e -> {
                    log.error("메시지 처리 실패, DLQ로 이동: {}", e.getMessage());
                    support.sendToDlq(record.value());
                    record.receiverOffset().acknowledge();
                    return Mono.empty();
                })
                .then();
    }

    /**
     * 각 락 전략별 주문 생성 로직. 서브클래스가 구현.
     */
    protected abstract Mono<Order> createOrder(OrderMessage orderMessage);
}