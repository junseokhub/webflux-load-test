package com.testing.load.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.kafka")
public record KafkaProperties(
        String bootstrapServers,
        Producer producer,
        Consumer consumer
) {
    public record Producer(
            String keySerializer,
            String valueSerializer
    ) {}

    public record Consumer(
            String groupId,
            String keyDeserializer,
            String valueDeserializer,
            String autoOffsetReset
    ) {}
}