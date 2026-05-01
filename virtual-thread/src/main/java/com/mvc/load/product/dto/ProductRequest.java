package com.mvc.load.product.dto;

public record ProductRequest(
        String name,
        Integer price,
        Integer stock
) {
}
