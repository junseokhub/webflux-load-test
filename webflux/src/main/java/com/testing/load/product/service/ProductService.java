package com.testing.load.product.service;

import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.product.domain.Product;
import com.testing.load.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Mono<Product> createProduct(String name, int price, int stock, Long createdBy) {
        return productRepository.save(
                Product.builder()
                        .name(name)
                        .price(price)
                        .stock(stock)
                        .createdBy(createdBy)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public Flux<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Mono<Product> findById(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));
    }
}