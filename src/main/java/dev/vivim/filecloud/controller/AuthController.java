package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.controller.api.AuthApi;
import dev.vivim.filecloud.dto.request.AuthorizationRequest;
import dev.vivim.filecloud.dto.request.RegisterUserRequest;
import dev.vivim.filecloud.dto.response.UserResponse;
import dev.vivim.filecloud.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    @Override
    public UserResponse register(RegisterUserRequest request, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        log.info("Registation Request: {}", request.username());
        UserResponse response = userService.register(request);
        authAndSaveSession(request.username(), request.password(), httpReq, httpResp);
        return response;
    }


    @Override
    public UserResponse authorization(AuthorizationRequest request, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        log.info("Authorization Request: {}", request.username());
        UserResponse response = new UserResponse(request.username());
        authAndSaveSession(request.username(), request.password(), httpReq, httpResp);
        return response;
    }

    private void authAndSaveSession(String username, String password, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password)
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        contextRepository.saveContext(context, httpReq, httpResp);
    }
}
