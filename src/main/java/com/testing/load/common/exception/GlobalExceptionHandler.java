package com.testing.load.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage(), e);
        return Mono.just(ResponseEntity
                .status(e.getStatus())
                .body(ErrorResponse.of(e.getStatus().value(), e.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        return Mono.just(ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(500, "Internal Server Error!!!")));
    }
}
