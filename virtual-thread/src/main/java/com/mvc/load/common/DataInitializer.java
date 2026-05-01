package com.mvc.load.common;

import com.mvc.load.common.properties.AppProperties;
import com.mvc.load.order.OrderRepository;
import com.mvc.load.order.consumer.KafkaConsumerSupport;
import com.mvc.load.outbox.OutboxRepository;
import com.mvc.load.product.Product;
import com.mvc.load.product.ProductRepository;
import com.mvc.load.user.Role;
import com.mvc.load.user.User;
import com.mvc.load.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaConsumerSupport kafkaConsumerSupport;
    private final AppProperties appProperties;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;



    @Override
    public void run(ApplicationArguments args) {
        int initialStock = appProperties.dataInit().productInitialStock();

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("asdf123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("admin 유저 생성 완료");

            Product product = Product.builder()
                    .name("테스트 상품")
                    .price(10000)
                    .stock(initialStock)
                    .createdBy(admin)
                    .build();
            productRepository.save(product);
            log.info("테스트 상품 생성 완료");
        }

        if (!appProperties.dataInit().enabled()) {
            log.info("데이터 초기화 스킵");
            return;
        }

        // 1. orders 초기화
        orderRepository.deleteAll();
        log.info("orders 초기화 완료");

        outboxRepository.deleteAll();
        log.info("outbox 초기화 완료");

        // 2. DB stock 리셋
        jdbcTemplate.update("UPDATE products SET stock = ?", initialStock);
        log.info("상품 DB stock 리셋 완료: stock={}", initialStock);

        // 3. Redis stock 초기화
        productRepository.findAll().forEach(product -> {
            String stockKey = "{product:" + product.getId() + "}:stock";
            redisTemplate.opsForValue().set(stockKey, String.valueOf(initialStock));
            log.info("상품 ID: {} Redis stock 초기화 완료: {}", product.getId(), initialStock);
        });

        // 4. KafkaConsumerSupport 초기화
        kafkaConsumerSupport.reset();
        log.info("KafkaConsumerSupport 초기화 완료");
    }
}