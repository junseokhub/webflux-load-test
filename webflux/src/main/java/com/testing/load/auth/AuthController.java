package com.testing.load.auth;

import com.testing.load.auth.dto.LoginAuthResponse;
import com.testing.load.auth.dto.LoginRequest;
import com.testing.load.auth.dto.RegisterRequest;
import com.testing.load.auth.dto.TokenResponse;
import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.common.properties.JwtProperties;
import com.testing.load.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    // 회원가입
    // Service는 User 반환 → Controller에서 UserResponseDto로 변환
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponseDto> register(@RequestBody RegisterRequest request) {
        return authService.register(request.username(), request.password())
                .map(UserResponseDto::from);
    }

    // 로그인
    // 1. Service에서 TokenResult(accessToken, refreshToken) 받음
    // 2. refreshToken → HttpOnly Cookie
    // 3. accessToken만 바디에
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginAuthResponse>> login(
            @RequestBody LoginRequest request,
            ServerWebExchange exchange) {
        return authService.login(request.getUsername(), request.getPassword())
                .map(result -> {
                    exchange.getResponse().addCookie(buildRefreshCookie(
                            result.refreshToken(),
                            Duration.ofMillis(jwtProperties.refreshTokenExpiration())
                    ));
                    return ResponseEntity.ok(new LoginAuthResponse(result.user().id(), result.user().username(), result.accessToken()));
                });
    }

    // 로그아웃
    // 1. 액세스 토큰 블랙리스트 등록
    // 2. Redis 리프레시 토큰 삭제
    // 3. Cookie 즉시 만료
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(
            @AuthenticationPrincipal Mono<Long> userId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            ServerWebExchange exchange) {
        String accessToken = bearerToken.substring(7);

        // maxAge(0) → Cookie 즉시 만료
        exchange.getResponse().addCookie(buildRefreshCookie("", Duration.ZERO));

        return userId.flatMap(id -> authService.logout(id, accessToken));
    }

    // 토큰 재발급
    // 1. Cookie에서 리프레시 토큰 꺼내기
    // 2. 새 액세스 + 리프레시 토큰 발급
    // 3. 새 리프레시 토큰 Cookie 갱신
    // 4. 새 액세스 토큰만 바디에
    @PostMapping("/reissue")
    public Mono<ResponseEntity<TokenResponse>> reissue(ServerWebExchange exchange) {
        var refreshCookie = exchange.getRequest().getCookies().getFirst("refresh_token");

        if (refreshCookie == null) {
            return Mono.error(new BusinessException(ErrorCode.TOKEN_INVALID));
        }

        return authService.reissue(refreshCookie.getValue())
                .map(result -> {
                    // 새 리프레시 토큰으로 Cookie 갱신 (Rotation)
                    exchange.getResponse().addCookie(buildRefreshCookie(
                            result.refreshToken(),
                            Duration.ofMillis(jwtProperties.refreshTokenExpiration())
                    ));
                    return ResponseEntity.ok(new TokenResponse(result.accessToken()));
                });
    }

    // ResponseCookie 생성 공통 메서드
    // 로그인, 재발급, 로그아웃 모두 동일한 Cookie 설정 사용
    // value: "" + maxAge: 0 → 로그아웃 시 Cookie 즉시 만료
    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from("refresh_token", value)
                // JavaScript 접근 불가 → XSS 방어
                .httpOnly(true)
                // 운영환경에서는 반드시 true로 변경
                .secure(false)
                // CSRF 방어
                .sameSite("Strict")
                // /api/auth 경로에서만 Cookie 전송
                // reissue, logout 요청 시에만 Cookie 포함
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}