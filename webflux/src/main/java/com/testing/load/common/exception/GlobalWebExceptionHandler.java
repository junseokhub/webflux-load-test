package com.testing.load.common.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@Order(-2)
// 필터 레벨 예외는 RestControllerAdvice가 잡지 못한다 Webflux에서 필터예외는 해당 클래스에서
@RequiredArgsConstructor
public class GlobalWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // BusinessException → 우리가 정의한 커스텀 예외
        log.error("GlobalWebExceptionHandler caught: {}", ex.getMessage(), ex);

        if (ex instanceof BusinessException businessException) {
            return writeErrorResponse(exchange, businessException.getStatus(), businessException.getMessage());
        }

        // 그 외 예외 → 500 Internal Server Error
        return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
    }

    // 에러 응답 직접 작성
    // Webflux는 HttpServletResponse 없고 ServerWebExchange로 응답
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.of(status.value(), message);

        byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}