package com.testing.load.auth;

import com.testing.load.auth.dto.TokenResult;
import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.common.jwt.JwtProvider;
import com.testing.load.common.jwt.TokenBlacklistService;
import com.testing.load.common.properties.JwtProperties;
import com.testing.load.user.UserRepository;
import com.testing.load.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // Redis key 상수로 관리 → 오타 방지 + 변경 시 한 곳에서만 수정
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // 회원가입
    @Transactional
    public Mono<User> register(String username, String password) {
        return userRepository.findByUsername(username)
                .flatMap(existing -> Mono.<User>error(new BusinessException(ErrorCode.USER_ALREADY_EXISTS)))
                .switchIfEmpty(Mono.defer(() -> userRepository.save(
                        User.builder()
                                .username(username)
                                .password(passwordEncoder.encode(password))
                                .build()
                )));
    }

    // 로그인
    // 1. username으로 유저 조회
    // 2. 비밀번호 검증
    // 3. 액세스 + 리프레시 토큰 발급
    // 4. 리프레시 토큰 Redis 저장
    @Transactional(readOnly = true)
    public Mono<TokenResult> login(String username, String password) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        return Mono.error(new BusinessException(ErrorCode.PASSWORD_MISMATCH));
                    }
                    String accessToken = jwtProvider.generateAccessToken(user.getId());
                    // saveRefreshToken → 공통 메서드로 분리 (로그인/재발급 공통 사용)
                    return saveRefreshToken(user.getId())
                            .map(refreshToken -> new TokenResult(accessToken, refreshToken));
                });
    }

    // 로그아웃
    // 1. 액세스 토큰 블랙리스트 등록
    // 2. Redis 리프레시 토큰 삭제
    @Transactional
    public Mono<Void> logout(Long userId, String accessToken) {
        long expiration = jwtProvider.getClaims(accessToken)
                .getExpiration()
                .getTime() - System.currentTimeMillis();

        return tokenBlacklistService.addBlacklist(accessToken, Duration.ofMillis(expiration))
                .then(reactiveRedisTemplate.opsForValue()
                        .delete(REFRESH_TOKEN_PREFIX + userId)
                        .then());
    }

    // 토큰 재발급
    // 1. 리프레시 토큰 유효성 검증
    // 2. Redis 저장 토큰과 비교 → 탈취 감지
    // 3. 새 액세스 + 리프레시 토큰 발급 (Rotation)
    public Mono<TokenResult> reissue(String refreshToken) {
        try {
            jwtProvider.validateToken(refreshToken);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.TOKEN_EXPIRED) {
                return Mono.error(new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED));
            }
            return Mono.error(e);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        return reactiveRedisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.TOKEN_INVALID)))
                .flatMap(savedToken -> {
                    // 탈취 감지 → Redis 저장 토큰과 다르면 즉시 삭제 후 거부
                    if (!savedToken.equals(refreshToken)) {
                        return reactiveRedisTemplate.opsForValue()
                                .delete(REFRESH_TOKEN_PREFIX + userId)
                                .then(Mono.error(new BusinessException(ErrorCode.TOKEN_INVALID)));
                    }

                    String newAccessToken = jwtProvider.generateAccessToken(userId);
                    // Rotation → 재발급 시 리프레시 토큰도 새로 발급
                    return saveRefreshToken(userId)
                            .map(newRefreshToken -> new TokenResult(newAccessToken, newRefreshToken));
                });
    }

    // 리프레시 토큰 저장 공통 메서드
    // 로그인/재발급 모두 동일한 로직 사용
    // Redis 저장 실패 시 예외 처리 → 로그 남기고 서버 에러 반환
    private Mono<String> saveRefreshToken(Long userId) {
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        return reactiveRedisTemplate.opsForValue()
                .set(
                        REFRESH_TOKEN_PREFIX + userId,
                        refreshToken,
                        Duration.ofMillis(jwtProperties.refreshTokenExpiration())
                )
                .doOnError(e -> log.error("Redis refreshToken 저장 실패: {}", e.getMessage()))
                .onErrorMap(e -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR))
                .thenReturn(refreshToken);
    }
}