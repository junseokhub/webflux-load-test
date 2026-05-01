package com.testing.load.common.config;

import com.testing.load.common.properties.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.bootstrapServers());
        return new KafkaAdmin(config);
    }

    @Bean
    public KafkaSender<String, String> kafkaSender() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProperties.producer().keySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProperties.producer().valueSerializer());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        return KafkaSender.create(SenderOptions.create(props));
    }

    /**
     * order-requests 토픽 구독 (Consumer들이 사용)
     */
    @Bean
    public ReceiverOptions<String, String> orderRequestReceiverOptions() {
        return baseReceiverOptions(kafkaProperties.consumer().groupId())
                .subscription(List.of("order-requests"));
    }

    /**
     * order-results 토픽 구독 (KafkaSyncOrderService가 사용)
     */
    @Bean
    public ReceiverOptions<String, String> resultReceiverOptions() {
        return baseReceiverOptions("order-sync-group")
                .subscription(List.of("order-results"));
    }

    private ReceiverOptions<String, String> baseReceiverOptions(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.bootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaProperties.consumer().keyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaProperties.consumer().valueDeserializer());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.consumer().autoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

        return ReceiverOptions.create(props);
    }

    @Bean
    public NewTopic orderRequestsTopic() {
        return TopicBuilder.name("order-requests")
                .partitions(3)
                .replicas(3)
                .build();
    }

    @Bean
    public NewTopic orderResultsTopic() {
        return TopicBuilder.name("order-results")
                .partitions(3)
                .replicas(3)
                .build();
    }

    @Bean
    public NewTopic orderRequestsDlqTopic() {
        return TopicBuilder.name("order-requests-dlq")
                .partitions(3)
                .replicas(3)
                .build();
    }
}