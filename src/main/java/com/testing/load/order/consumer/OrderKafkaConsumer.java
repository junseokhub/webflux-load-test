package com.testing.load.order.consumer;

public interface OrderKafkaConsumer {
    void consume(String message);
}