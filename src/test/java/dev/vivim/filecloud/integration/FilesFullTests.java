package dev.vivim.filecloud.integration;

import dev.vivim.filecloud.dto.RegisterUserRequest;
import dev.vivim.filecloud.exception.ResourceNotFoundException;
import dev.vivim.filecloud.service.FileService;
import dev.vivim.filecloud.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FilesFullTests extends BaseIntegrationTest {
    @Autowired UserService userService;
    @Autowired FileService fileService;

    @AfterEach
    void clearUpS3() {
        try {
            for (S3Object obj : s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).prefix("testuser/").build()).contents()) {
                //s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(obj.key()).build());
                // - не имеет смысла, т.к. используем Testcontainers
            }
        }
        catch (SdkClientException ignored) {}
    }

    @Test
    void shouldCreateFileInMinio() throws IOException {
        userService.register(new RegisterUserRequest("testuser", "password"));
        byte[] bytes = new byte[] {'h', 'e', 'l', 'l', 'o'};
        MultipartFile multipartFile = new MockMultipartFile("file", "file.txt", "text/plain", bytes);
        String s3Key = "testuser/path/file.txt";
        fileService.uploadResource("path/","testuser", List.of(multipartFile));
        var responseBytes = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3Key).build()).readAllBytes();
        System.out.println("Response bytes: "+Arrays.toString(responseBytes));
        Assertions.assertArrayEquals(responseBytes, bytes);
    }

    @Test
    void shouldCreateFileForOnlyOneUser() throws IOException {
        String user1 = "testuser";
        String user2 = "testuser2";
        userService.register(new RegisterUserRequest(user1, "password"));

        byte[] bytes = new byte[] {'h', 'e', 'l', 'l', 'o'};
        MultipartFile multipartFile = new MockMultipartFile("file", "file.txt", "text/plain", bytes);
        fileService.uploadResource("path/",user1, List.of(multipartFile));

        userService.register(new RegisterUserRequest(user2, "password"));
        Assertions.assertEquals("file.txt", fileService.getResource("path/file.txt", user1).name());
        Assertions.assertThrows(ResourceNotFoundException.class, () -> fileService.getResource("path/file.txt", user2));
    }
}
