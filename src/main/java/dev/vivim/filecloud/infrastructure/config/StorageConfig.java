package dev.vivim.filecloud.infrastructure.config;

import dev.vivim.filecloud.infrastructure.storage.ObjectStorage;
import dev.vivim.filecloud.infrastructure.storage.S3ObjectStorage;
import dev.vivim.filecloud.minio.s3.S3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class StorageConfig {
    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "aws")
    public ObjectStorage s3ObjectStorage(S3Client s3Client, S3Properties s3Properties) {
        return new S3ObjectStorage(s3Client, s3Properties);
    }
}
