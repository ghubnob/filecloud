package dev.vivim.filecloud.dto.request;

import dev.vivim.filecloud.dto.annotation.ValidPassword;
import dev.vivim.filecloud.dto.annotation.ValidUsername;

public record RegisterUserRequest(
        @ValidUsername
        String username,

        @ValidPassword
        String password) {}
