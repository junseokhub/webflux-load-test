package com.testing.load.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(DataInit dataInit) {
    public record DataInit(boolean enabled, int productInitialStock) {
    }
}
