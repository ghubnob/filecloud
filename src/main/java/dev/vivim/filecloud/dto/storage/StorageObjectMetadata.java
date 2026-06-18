package dev.vivim.filecloud.dto.storage;

public record StorageObjectMetadata(Long size, boolean isDirectory, String contentType) {
}
