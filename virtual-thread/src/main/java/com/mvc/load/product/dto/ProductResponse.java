package com.mvc.load.product.dto;

public record ProductResponse(
        String name,
        Integer price,
        Integer stock
) {

    public static ProductResponse of(String name, Integer price, Integer stock) {
        return new ProductResponse(name, price, stock);
    }
}
