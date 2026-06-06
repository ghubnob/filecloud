package dev.vivim.filecloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @Size(min=3,max=20,message="Username must be from 3 to 20 letters!")
        @NotBlank(message="Username must be from 3 to 20 letters!")
        String username,

        @Size(min=8,max=30,message="Password must be from 8 to 30 letters!")
        @NotBlank(message="Username must be from 8 to 30 letters!")
        String password) {}
