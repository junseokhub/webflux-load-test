package com.testing.load.common.config;

import com.testing.load.common.properties.FlywayProperties;
import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(FlywayProperties flywayProperties) {
        if (!flywayProperties.enabled()) {
            return null;
        }
        return Flyway.configure()
                .dataSource(
                        flywayProperties.url(),
                        flywayProperties.user(),
                        flywayProperties.password()
                )
                .locations(flywayProperties.locations().toArray(String[]::new))
                .baselineVersion("0")
                .baselineOnMigrate(true)
                .load();
    }

}