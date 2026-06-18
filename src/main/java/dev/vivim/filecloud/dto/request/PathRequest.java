package dev.vivim.filecloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PathRequest(@NotBlank(message = "Path must not be blank!")
                          String path) {}
