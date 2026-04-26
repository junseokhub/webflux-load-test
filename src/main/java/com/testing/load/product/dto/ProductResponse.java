package com.testing.load.product.dto;

import com.testing.load.product.domain.Product;

public record ProductResponse(
        Long id,
        String name,
        int price,
        int stock,
        Long createdBy
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCreatedBy()
        );
    }
}