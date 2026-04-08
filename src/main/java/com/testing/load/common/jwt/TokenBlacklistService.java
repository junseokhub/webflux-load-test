package com.testing.load.common.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // 블랙리스트에 액세스 토큰 등록
    // TTL → 토큰 남은 만료시간만큼만 저장 → 만료되면 자동 삭제
    public Mono<Boolean> addBlacklist(String accessToken, Duration ttl) {
        return reactiveRedisTemplate.opsForValue()
                .set("blacklist:" + accessToken, "logout", ttl);
    }

    // 블랙리스트 여부 확인
    public Mono<Boolean> isBlacklisted(String accessToken) {
        return reactiveRedisTemplate.hasKey("blacklist:" + accessToken);
    }
}