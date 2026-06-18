package dev.vivim.filecloud.dto;

import dev.vivim.filecloud.minio.s3.S3Properties;

public record UserStorageRoot(String value) {
    public static UserStorageRoot forUser(S3Properties properties, Integer userId) {
        return new UserStorageRoot(properties.userRootDirectory().formatted(userId));
    }
}
