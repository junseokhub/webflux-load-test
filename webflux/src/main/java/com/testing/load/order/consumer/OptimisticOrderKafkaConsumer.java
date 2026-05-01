package com.testing.load.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.order.domain.Order;
import com.testing.load.order.dto.OrderMessage;
import com.testing.load.order.service.OptimisticLockOrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOptions;

public class OptimisticOrderKafkaConsumer extends AbstractOrderKafkaConsumer {

    private final OptimisticLockOrderService service;

    public OptimisticOrderKafkaConsumer(
            OptimisticLockOrderService service,
            KafkaConsumerSupport support,
            ObjectMapper objectMapper,
            @Qualifier("orderRequestReceiverOptions") ReceiverOptions<String, String> receiverOptions
    ) {
        super(support, objectMapper, receiverOptions);
        this.service = service;
    }

    @Override
    protected Mono<Order> createOrder(OrderMessage msg) {
        return service.createOrder(msg.userId(), msg.productId(), msg.couponIssueId(), msg.correlationId());
    }
}