package dev.vivim.filecloud.dto;

import dev.vivim.filecloud.dto.annotation.ValidPassword;
import dev.vivim.filecloud.dto.annotation.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @ValidUsername
        String username,

        @ValidPassword
        String password) {}
