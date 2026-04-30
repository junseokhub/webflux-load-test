package com.testing.load.auth.dto;

public record LoginAuthResponse(
        Long id,
        String username,
        String accessToken
) {
}
