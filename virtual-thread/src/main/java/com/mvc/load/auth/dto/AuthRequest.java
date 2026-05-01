package com.mvc.load.auth.dto;

public record AuthRequest(
        String username,
        String password
) {
}
