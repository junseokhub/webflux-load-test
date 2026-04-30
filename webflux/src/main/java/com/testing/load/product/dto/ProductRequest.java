package com.testing.load.product.dto;

public record ProductRequest(
        String name,
        int price,
        int stock
) {}