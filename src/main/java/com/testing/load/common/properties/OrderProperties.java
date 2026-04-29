package com.testing.load.common.properties;

import com.testing.load.order.service.OrderConsumerType;
import com.testing.load.order.service.OrderServiceType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order")
public record OrderProperties(
        OrderServiceType serviceType,
        OrderConsumerType consumerType
        ) {}