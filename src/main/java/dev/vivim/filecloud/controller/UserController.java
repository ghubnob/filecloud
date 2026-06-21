package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.api.UserApi;
import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class UserController implements UserApi {
    @Override
    public UserResponse getMe(AuthenticatedUser user) {
        log.info("Get authorized user's name request: {}", user.getUsername());
        return new UserResponse(user.username());
    }
}
