package com.mvc.load.auth;

import com.mvc.load.auth.dto.TokenResponse;
import com.mvc.load.common.exception.BusinessException;
import com.mvc.load.common.exception.ErrorCode;
import com.mvc.load.common.jwt.JwtProvider;
import com.mvc.load.user.Role;
import com.mvc.load.user.User;
import com.mvc.load.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public TokenResponse register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        return generateTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        return generateTokens(user);
    }

    private TokenResponse generateTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        return new TokenResponse(accessToken, refreshToken);
    }
}
