package dev.vivim.filecloud.integration;

import dev.vivim.filecloud.TestcontainersConfig;
import dev.vivim.filecloud.minio.s3.S3Properties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public abstract class BaseIntegrationTest {
    protected static final GenericContainer<?> minioContainer;
    static {
        minioContainer = new GenericContainer<>("minio/minio:latest")
                .withCommand("server /data")
                .withEnv("MINIO_ROOT_USER", "minioadmin")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                .withExposedPorts(9000);
        minioContainer.start();
    }

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        String endpoint = "http://" + minioContainer.getHost() + ":" + minioContainer.getMappedPort(9000);
        registry.add("aws.s3.endpoint", () -> endpoint);
        registry.add("aws.s3.access-key", () -> "minioadmin");
        registry.add("aws.s3.secret-key", () -> "minioadmin");
    }

    @Autowired
    protected S3Client s3Client;

    @Autowired
    protected S3Properties s3properties;

    @BeforeAll
    void initMinioBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(s3properties.bucketName()).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(s3properties.bucketName()).build());
                log.debug("Test bucket created: {}", s3properties.bucketName());
            } else {
                throw e;
            }
        }
    }
}
