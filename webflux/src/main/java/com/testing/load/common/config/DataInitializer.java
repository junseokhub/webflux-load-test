package com.testing.load.common.config;

import com.testing.load.common.properties.AppProperties;
import com.testing.load.coupon.repository.CouponIssueRepository;
import com.testing.load.coupon.repository.CouponRepository;
import com.testing.load.order.consumer.KafkaConsumerSupport;
import com.testing.load.order.repository.OrderRepository;
import com.testing.load.outbox.repository.OutboxRepository;
import com.testing.load.product.repository.ProductRepository;
import com.testing.load.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final AppProperties appProperties;
    private final KafkaConsumerSupport kafkaConsumerSupport;
    private final OutboxRepository outboxRepository;
    private final DatabaseClient databaseClient;

    @Override
    public void run(ApplicationArguments args) {
        if (!appProperties.dataInit().enabled()) {
            log.info("데이터 초기화 스킵");
            return;
        }

        // 1. orders 초기화 (FK 때문에 coupon_issues보다 먼저)
        orderRepository.deleteAll()
                .doOnSuccess(r -> log.info("orders 초기화 완료"))
                .doOnError(e -> log.error("orders 초기화 실패: {}", e.getMessage()))
                .subscribe();

        // 2. coupon_issues 초기화
        couponIssueRepository.deleteAll()
                .doOnSuccess(r -> log.info("coupon_issues 초기화 완료"))
                .doOnError(e -> log.error("coupon_issues 초기화 실패: {}", e.getMessage()))
                .subscribe();

        // 3. Redis refresh 토큰 초기화
        userRepository.findAll()
                .flatMap(user -> reactiveRedisTemplate.delete("refresh:" + user.getId()))
                .count()
                .doOnSuccess(count -> log.info("refresh 토큰 초기화 완료: {}개", count))
                .doOnError(e -> log.error("refresh 토큰 삭제 실패: {}", e.getMessage()))
                .subscribe();

        // 4. 쿠폰 Redis 재고 초기화
        couponRepository.findAll()
                .flatMap(coupon -> {
                    String stockKey = "{coupon:" + coupon.getId() + "}:stock";
                    String usersKey = "{coupon:" + coupon.getId() + "}:users";

                    return reactiveRedisTemplate.opsForValue()
                            .set(stockKey, String.valueOf(coupon.getTotalStock()))
                            .then(reactiveRedisTemplate.delete(usersKey))
                            .doOnSuccess(r -> log.info(
                                    "쿠폰 ID: {} 이름: {} 재고: {} Redis 초기화 완료",
                                    coupon.getId(), coupon.getName(), coupon.getTotalStock()
                            ));
                })
                .doOnError(e -> log.error("쿠폰 Redis 초기화 실패: {}", e.getMessage()))
                .subscribe();

        // 5. 상품 DB stock 리셋 → Redis 재고 초기화 (순서 보장)
        int initialStock = appProperties.dataInit().productInitialStock();
        databaseClient.sql("UPDATE products SET stock = :stock")
                .bind("stock", initialStock)
                .fetch()
                .rowsUpdated()
                .doOnSuccess(rows -> log.info("상품 DB stock 리셋 완료: {}개 행, stock={}", rows, initialStock))
                .flatMapMany(ignored -> productRepository.findAll())
                .flatMap(product -> {
                    String stockKey = "{product:" + product.getId() + "}:stock";
                    return reactiveRedisTemplate.opsForValue()
                            .set(stockKey, String.valueOf(initialStock))
                            .doOnSuccess(r -> log.info(
                                    "상품 ID: {} 이름: {} 재고: {} Redis 초기화 완료",
                                    product.getId(), product.getName(), initialStock
                            ));
                })
                .doOnError(e -> log.error("상품 재고 초기화 실패: {}", e.getMessage()))
                .subscribe();

        // 6. KafkaConsumerSupport 초기화
        kafkaConsumerSupport.reset();

        orderRepository.deleteAll()
                .doOnSuccess(r -> log.info("orders 초기화 완료"))
                .doOnError(e -> log.error("orders 초기화 실패: {}", e.getMessage()))
                .subscribe();

        outboxRepository.deleteAll()
                .doOnSuccess(r -> log.info("outbox 초기화 완료"))
                .doOnError(e -> log.error("outbox 초기화 실패: {}", e.getMessage()))
                .subscribe();
        log.info("KafkaConsumerSupport 초기화 완료");
    }
}
