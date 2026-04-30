package com.testing.load.outbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.order.service.RedisLuaOrderService;
import com.testing.load.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 1000)
    public void processOutbox() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")
                .flatMap(outbox -> {
                    // 먼저 PROCESSING으로 변경해서 중복 처리 방지
                    outbox.markProcessing();
                    return outboxRepository.save(outbox);
                })
                .flatMap(outbox -> {
                    try {
                        RedisLuaOrderService.OutboxPayload payload = objectMapper.readValue(
                                outbox.getPayload(), RedisLuaOrderService.OutboxPayload.class);

                        return databaseClient.sql("UPDATE products SET stock = stock - 1 WHERE id = :id AND stock > 0")
                                .bind("id", payload.productId())
                                .fetch()
                                .rowsUpdated()
                                .flatMap(rows -> {
                                    outbox.markProcessed();
                                    return outboxRepository.save(outbox);
                                })
                                .onErrorResume(e -> {
                                    log.error("Outbox 처리 실패: id={}, error={}", outbox.getId(), e.getMessage());
                                    outbox.markFailed(e.getMessage());
                                    return outboxRepository.save(outbox);
                                });
                    } catch (Exception e) {
                        outbox.markFailed(e.getMessage());
                        return outboxRepository.save(outbox);
                    }
                })
                .doFinally(signal -> running.set(false))
                .subscribe();
    }
}