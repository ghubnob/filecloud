package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Users", description = "Просмотр информации о пользователях")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Получение usename пользователя")
    @ApiResponse(responseCode = "200", description = "Username пользователя получен")
    @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    public UserResponse getMe(@AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Get authorized user's name request: {}", user.getUsername());
        return new UserResponse(user.username());
    }
}
