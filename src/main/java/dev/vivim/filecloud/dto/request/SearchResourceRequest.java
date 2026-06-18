package dev.vivim.filecloud.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SearchResourceRequest(@NotBlank(message = "Search query must not be blank!") String query) {}
