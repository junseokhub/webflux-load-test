package com.testing.load.outbox.repository;

import com.testing.load.outbox.domain.Outbox;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OutboxRepository extends ReactiveCrudRepository<Outbox, Long> {
    Flux<Outbox> findByStatusOrderByCreatedAtAsc(String status);
}