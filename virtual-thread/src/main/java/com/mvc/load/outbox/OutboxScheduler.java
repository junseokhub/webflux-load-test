package com.mvc.load.outbox;

import com.mvc.load.order.service.RedisLuaOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 100)
    public void processOutbox() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")
                    .forEach(outbox -> {
                        outbox.markProcessing();
                        outboxRepository.save(outbox);

                        try {
                            RedisLuaOrderService.OutboxPayload payload = objectMapper.readValue(
                                    outbox.getPayload(), RedisLuaOrderService.OutboxPayload.class);

                            int rows = jdbcTemplate.update(
                                    "UPDATE products SET stock = stock - 1 WHERE id = ? AND stock > 0",
                                    payload.productId()
                            );

                            if (rows > 0) {
                                outbox.markProcessed();
                            } else {
                                outbox.markFailed("재고 소진");
                            }
                            outboxRepository.save(outbox);

                        } catch (Exception e) {
                            log.error("Outbox 처리 실패: id={}, error={}", outbox.getId(), e.getMessage());
                            outbox.markFailed(e.getMessage());
                            outboxRepository.save(outbox);
                        }
                    });
        } finally {
            running.set(false);
        }
    }
}