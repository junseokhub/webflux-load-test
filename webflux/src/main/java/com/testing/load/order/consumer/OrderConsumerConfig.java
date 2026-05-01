package com.testing.load.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.common.properties.OrderProperties;
import com.testing.load.order.service.OptimisticLockOrderService;
import com.testing.load.order.service.OrderConsumerType;
import com.testing.load.order.service.PessimisticLockOrderService;
import com.testing.load.order.service.RedisLuaOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.ReceiverOptions;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderConsumerConfig {

    private final OrderProperties orderProperties;
    private final OptimisticLockOrderService optimisticLockOrderService;
    private final PessimisticLockOrderService pessimisticLockOrderService;
    private final RedisLuaOrderService redisLuaOrderService;
    private final KafkaConsumerSupport kafkaConsumerSupport;
    private final ObjectMapper objectMapper;

    @Bean
    public OrderKafkaConsumer orderKafkaConsumer(
            @Qualifier("orderRequestReceiverOptions") ReceiverOptions<String, String> receiverOptions
    ) {
        OrderConsumerType type = orderProperties.consumerType();

        if (type == OrderConsumerType.NONE) {
            log.info("OrderKafkaConsumer 비활성화");
            return null;
        }

        OrderKafkaConsumer consumer = switch (type) {
            case OPTIMISTIC -> new OptimisticOrderKafkaConsumer(
                    optimisticLockOrderService, kafkaConsumerSupport, objectMapper, receiverOptions);
            case PESSIMISTIC -> new PessimisticOrderKafkaConsumer(
                    pessimisticLockOrderService, kafkaConsumerSupport, objectMapper, receiverOptions);
            case LUA -> new LuaOrderKafkaConsumer(
                    redisLuaOrderService, kafkaConsumerSupport, objectMapper, receiverOptions);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        log.info("OrderKafkaConsumer 구현체: {}", consumer.getClass().getSimpleName());
        return consumer;
    }
}