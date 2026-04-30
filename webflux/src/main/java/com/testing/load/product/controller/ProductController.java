package com.testing.load.product.controller;

import com.testing.load.common.response.ApiResponse;
import com.testing.load.product.dto.ProductRequest;
import com.testing.load.product.dto.ProductResponse;
import com.testing.load.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<ProductResponse>> createProduct(
            @RequestBody ProductRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return productService.createProduct(
                request.name(),
                request.price(),
                request.stock(),
                userId
        ).map(ProductResponse::from).map(ApiResponse::ok);
    }

    @GetMapping
    public Flux<ProductResponse> findAll() {
        return productService.findAll()
                .map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ProductResponse::from)
                .map(ApiResponse::ok);
    }
}