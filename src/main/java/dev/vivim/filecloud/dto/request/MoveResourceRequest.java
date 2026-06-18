package dev.vivim.filecloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MoveResourceRequest(@NotBlank(message = "Resource path must not be blank!") String from,
                                  @NotBlank(message = "Resource path must not be blank!") String to) {}
