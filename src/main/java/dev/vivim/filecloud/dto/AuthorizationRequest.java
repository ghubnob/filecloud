package dev.vivim.filecloud.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorizationRequest(
        @NotBlank(message="Username must not be blank!")
        String username,

        @NotBlank(message="Username must not be blank!")
        String password) {}
