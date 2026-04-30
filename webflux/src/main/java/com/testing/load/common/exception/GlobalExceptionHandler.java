package com.testing.load.common.exception;

import com.testing.load.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(e.getStatus().value(), e.getMessage());
        return Mono.just(ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.fail(errorResponse)));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(500, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        return Mono.just(ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(errorResponse)));
    }
}
