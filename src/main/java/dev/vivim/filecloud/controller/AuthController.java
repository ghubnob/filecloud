package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.dto.AuthorizationRequest;
import dev.vivim.filecloud.dto.RegisterUserRequest;
import dev.vivim.filecloud.dto.UserResponse;
import dev.vivim.filecloud.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Регистрация и авторизация пользователей")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация пользователя")
    @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @ApiResponse(responseCode = "409", description = "Пользователь с таким именем уже существует")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        log.info("Registation Request: {}", request.username());
        UserResponse response = userService.register(request);

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        contextRepository.saveContext(context, httpReq, httpResp);
        return response;
    }


    @PostMapping("/sign-in")
    @Operation(summary = "Авторизация пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь успешно авторизован")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    @ApiResponse(responseCode = "401", description = "Введены неверные данные")
    public UserResponse authorization(@Valid @RequestBody AuthorizationRequest request, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        log.info("Authorization Request: {}", request.username());
        UserResponse response = new UserResponse(request.username());

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        contextRepository.saveContext(context, httpReq, httpResp);
        return response;
    }
}
