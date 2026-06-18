package dev.vivim.filecloud.controller.api;

import dev.vivim.filecloud.dto.request.AuthorizationRequest;
import dev.vivim.filecloud.dto.request.RegisterUserRequest;
import dev.vivim.filecloud.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Регистрация и авторизация пользователей")
public interface AuthApi {
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация пользователя")
    @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @ApiResponse(responseCode = "409", description = "Пользователь с таким именем уже существует")
    UserResponse register(@Valid @RequestBody RegisterUserRequest request, HttpServletRequest httpReq, HttpServletResponse httpResp);

    @PostMapping("/sign-in")
    @Operation(summary = "Авторизация пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь успешно авторизован")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @ApiResponse(responseCode = "401", description = "Введены неверные данные")
    UserResponse authorization(@Valid @RequestBody AuthorizationRequest request, HttpServletRequest httpReq, HttpServletResponse httpResp);
}
