package com.testing.load.auth.dto;

public record RegisterRequest(
        String username,
        String password
) {
}
