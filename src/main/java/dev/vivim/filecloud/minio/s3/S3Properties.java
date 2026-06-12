package dev.vivim.filecloud.minio.s3;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        @NotEmpty String endpoint,
        @NotEmpty String accessKey,
        @NotEmpty String secretKey,
        @NotEmpty String region,
        @NotEmpty String bucketName
) {}
