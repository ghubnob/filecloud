package dev.vivim.filecloud.dto;

public record PathResponse(String path, String name, Long size, FileType type) {}