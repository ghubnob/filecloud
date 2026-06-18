package dev.vivim.filecloud.infrastructure.config;

import dev.vivim.filecloud.infrastructure.paths.PathResolver;
import dev.vivim.filecloud.infrastructure.paths.s3keys.S3PathResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PathConfig {
    @Bean
    public PathResolver pathResolver() {
        return new S3PathResolver();
    }
}
