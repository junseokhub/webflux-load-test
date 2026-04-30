package com.testing.load.user;

import com.testing.load.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Mono<UserResponseDto> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(UserResponseDto::from);
    }
}
