package com.testing.load.order.consumer;

import com.testing.load.common.properties.OrderProperties;
import com.testing.load.order.service.OptimisticLockOrderService;
import com.testing.load.order.service.OrderConsumerType;
import com.testing.load.order.service.PessimisticLockOrderService;
import com.testing.load.order.service.RedisLuaOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderConsumerConfig {

    private final OrderProperties orderProperties;
    private final OptimisticLockOrderService optimisticLockOrderService;
    private final PessimisticLockOrderService pessimisticLockOrderService;
    private final RedisLuaOrderService redisLuaOrderService;
    private final KafkaConsumerSupport kafkaConsumerSupport;

    @Bean
    public OrderKafkaConsumer orderKafkaConsumer() {
        if (orderProperties.consumerType() == OrderConsumerType.NONE) {
            log.info("OrderKafkaConsumer 비활성화");
            return null;
        }
        OrderKafkaConsumer consumer = switch (orderProperties.consumerType()) {
            case OPTIMISTIC -> new OptimisticOrderKafkaConsumer(
                    optimisticLockOrderService, kafkaConsumerSupport);
            case PESSIMISTIC -> new PessimisticOrderKafkaConsumer(
                    pessimisticLockOrderService, kafkaConsumerSupport);
            case LUA -> new LuaOrderKafkaConsumer(
                    redisLuaOrderService, kafkaConsumerSupport);
            default -> throw new IllegalStateException("Unexpected value: " + orderProperties.consumerType());
        };
        log.info("OrderKafkaConsumer 구현체: {}", consumer.getClass().getSimpleName());
        return consumer;
    }
}