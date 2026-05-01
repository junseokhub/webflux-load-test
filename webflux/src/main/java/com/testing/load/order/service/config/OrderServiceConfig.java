package com.testing.load.order.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.load.common.properties.OrderProperties;
import com.testing.load.order.service.*;
import com.testing.load.outbox.repository.OutboxRepository;
import com.testing.load.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderServiceConfig {

    private final OrderProperties orderProperties;
    private final ProductRepository productRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final DefaultOrderService defaultOrderService;
    private final DatabaseClient databaseClient;
    private final KafkaSender<String, String> kafkaSender;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // 각 락 서비스 빈을 개별 등록
    @Bean
    public RedisLuaOrderService redisLuaOrderService() {
        return new RedisLuaOrderService(
                productRepository, reactiveRedisTemplate, defaultOrderService, outboxRepository, objectMapper);
    }

    @Bean
    public OptimisticLockOrderService optimisticLockOrderService() {
        return new OptimisticLockOrderService(productRepository, defaultOrderService);
    }

    @Bean
    public PessimisticLockOrderService pessimisticLockOrderService() {
        return new PessimisticLockOrderService(productRepository, databaseClient, defaultOrderService);
    }

    @Bean
    public KafkaAsyncOrderService kafkaAsyncOrderService() {
        return new KafkaAsyncOrderService(kafkaSender, objectMapper);
    }

    @Bean
    public KafkaSyncOrderService kafkaSyncOrderService(
    ) {
        return new KafkaSyncOrderService(kafkaSender, objectMapper);
    }

    @Bean
    @Primary
    public OrderService orderService(
            RedisLuaOrderService redisLuaOrderService,
            OptimisticLockOrderService optimisticLockOrderService,
            PessimisticLockOrderService pessimisticLockOrderService,
            KafkaAsyncOrderService kafkaAsyncOrderService,
            KafkaSyncOrderService kafkaSyncOrderService
    ) {
        OrderService service = switch (orderProperties.serviceType()) {
            case REDIS_LUA -> redisLuaOrderService;
            case OPTIMISTIC_LOCK -> optimisticLockOrderService;
            case PESSIMISTIC_LOCK -> pessimisticLockOrderService;
            case KAFKA_ASYNC -> kafkaAsyncOrderService;
            case KAFKA_SYNC -> kafkaSyncOrderService;
        };
        log.info("OrderService 구현체: {}", service.getClass().getSimpleName());
        return service;
    }
}