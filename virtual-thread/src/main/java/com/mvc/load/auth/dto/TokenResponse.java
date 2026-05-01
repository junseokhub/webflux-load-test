package com.mvc.load.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
