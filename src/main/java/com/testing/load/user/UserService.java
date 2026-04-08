package com.testing.load.user;

import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import com.testing.load.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)));
    }
}
