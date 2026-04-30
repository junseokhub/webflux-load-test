package com.testing.load.order.service;

public enum OrderServiceType {
    REDIS_LUA,
    OPTIMISTIC_LOCK,
    PESSIMISTIC_LOCK,
    KAFKA_ASYNC,
    KAFKA_SYNC,
}