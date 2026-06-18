package dev.vivim.filecloud.api;

import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/user")
@Tag(name = "Users", description = "Просмотр информации о пользователях")
public interface UserApi {
    @GetMapping("/me")
    @Operation(summary = "Получение usename пользователя")
    @ApiResponse(responseCode = "200", description = "Username пользователя получен")
    @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    UserResponse getMe(@AuthenticationPrincipal AuthenticatedUser user);
}
