package com.testing.load.coupon.service;

import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.coupon.domain.CouponIssue;
import com.testing.load.coupon.repository.CouponIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponIssueRepository couponIssueRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // Lua 스크립트 → 원자적으로 중복 체크 + 재고 차감 동시 처리
    // KEYS[1] → {coupon:{couponId}}:users
    // KEYS[2] → {coupon:{couponId}}:stock
    // ARGV[1] → userId
    // 반환값: 0 = 중복, -1 = 재고 없음, 1 = 성공
    private static final String ISSUE_COUPON_SCRIPT = """
            local isDuplicate = redis.call('SISMEMBER', KEYS[1], ARGV[1])
            if isDuplicate == 1 then
                return 0
            end
            local stock = tonumber(redis.call('GET', KEYS[2]))
            if stock == nil or stock <= 0 then
                return -1
            end
            redis.call('SADD', KEYS[1], ARGV[1])
            redis.call('DECR', KEYS[2])
            return 1
            """;

    // 쿠폰 발급
    // 1. Lua 스크립트로 중복 체크 + 재고 차감 원자적 처리
    // 2. 성공 시 DB에 발급 내역 저장
    @Transactional
    public Mono<CouponIssue> issueCoupon(Long couponId, Long userId) {
        List<String> keys = List.of(usersKey(couponId), stockKey(couponId));

        return reactiveRedisTemplate.execute(
                        RedisScript.of(ISSUE_COUPON_SCRIPT, Long.class),
                        keys,
                        List.of(String.valueOf(userId))
                )
                .next()
                .flatMap(result -> {
                    if (result == 0L) {
                        return Mono.error(new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED));
                    }
                    if (result == -1L) {
                        return Mono.error(new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK));
                    }
                    return couponIssueRepository.save(
                            CouponIssue.builder()
                                    .couponId(couponId)
                                    .userId(userId)
                                    .build()
                    ).onErrorMap(DuplicateKeyException.class,
                            e -> new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED));
                });
    }

    private String stockKey(Long couponId) {
        return "{coupon:" + couponId + "}:stock";
    }

    private String usersKey(Long couponId) {
        return "{coupon:" + couponId + "}:users";
    }
}