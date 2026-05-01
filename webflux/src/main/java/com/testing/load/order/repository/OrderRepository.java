package com.testing.load.order.repository;

import com.testing.load.order.domain.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    Mono<Order> findByCorrelationId(String correlationId);
}
