package com.testing.load.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.flyway")
public record FlywayProperties(
        boolean enabled,
        String url,
        String user,
        String password,
        java.util.List<String> locations
) {
}