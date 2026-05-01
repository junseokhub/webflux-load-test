package com.testing.load.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.order.dto.OrderMessage;
import com.testing.load.order.dto.OrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerSupport {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private volatile long startTime = 0;

    public void reset() {
        successCount.set(0);
        startTime = 0;
    }

    public void handleError(Throwable e, OrderMessage orderMessage) {
        if (e instanceof BusinessException be &&
                (be.getErrorCode() == ErrorCode.PRODUCT_OUT_OF_STOCK ||
                        be.getErrorCode() == ErrorCode.PRODUCT_ORDER_FAILED)) {
            log.warn("주문 처리 불가: {}", e.getMessage());
        } else {
            log.error("주문 처리 최종 실패, DLQ로 이동: {}", e.getMessage());
            sendToDlq(orderMessage);
        }
    }

    public void sendToDlq(Object message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaSender.send(Mono.just(SenderRecord.create(
                            new ProducerRecord<>("order-requests-dlq", payload),
                            "dlq"
                    )))
                    .next()
                    .doOnSuccess(r -> log.warn("DLQ로 메시지 이동 완료"))
                    .doOnError(e -> log.error("DLQ 발행 실패: {}", e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("DLQ 직렬화 실패: {}", e.getMessage());
        }
    }

    public void sendResult(OrderResult result) {
        try {
            String payload = objectMapper.writeValueAsString(result);
            kafkaSender.send(Mono.just(SenderRecord.create(
                            new ProducerRecord<>("order-results", result.correlationId(), payload),
                            result.correlationId()
                    )))
                    .next()
                    .doOnSuccess(r -> trackProgress())
                    .doOnError(e -> log.error("order-results 발행 실패: {}", e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("결과 직렬화 실패: {}", e.getMessage());
        }
    }

    private void trackProgress() {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
            log.info("===== Consumer 처리 시작 =====");
        }
        int count = successCount.incrementAndGet();
        if (count % 1000 == 0) {
            log.info("===== 처리 진행: {}건 =====", count);
        }
        if (count >= 10000 && count < 10005) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("============================");
            log.info("✅ 주문 처리 완료: 10,000건");
            log.info("⏱ 총 소요 시간: {}ms ({}초)", elapsed, elapsed / 1000);
            log.info("============================");
        }
    }
}