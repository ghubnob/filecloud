package dev.vivim.filecloud.dto.response;

import dev.vivim.filecloud.dto.FileType;

public record PathResponse(String path, String name, Long size, FileType type) {}