package com.testing.load.auth.dto;

import com.testing.load.user.dto.UserResponseDto;

public record LoginTokenResult(
        String accessToken,
        String refreshToken,
        UserResponseDto user
) {
}
