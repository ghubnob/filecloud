package dev.vivim.filecloud.integration;

import dev.vivim.filecloud.dto.request.RegisterUserRequest;
import dev.vivim.filecloud.exception.ResourceNotFoundException;
import dev.vivim.filecloud.repository.UserRepository;
import dev.vivim.filecloud.service.impl.S3FileServiceImpl;
import dev.vivim.filecloud.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.util.List;

public class FilesFullTests extends BaseIntegrationTest {
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired
    S3FileServiceImpl s3FileServiceImpl;

    @Test
    void shouldCreateFileInMinio() throws IOException {
        userService.register(new RegisterUserRequest("testuser", "password"));
        byte[] bytes = new byte[] {'h', 'e', 'l', 'l', 'o'};
        MultipartFile multipartFile = new MockMultipartFile("file", "file.txt", "text/plain", bytes);
        Integer savedUserId = userRepository.findByUsername("testuser")
                .orElseThrow(() -> new AssertionError("User was not saved!")).getId();
        Assertions.assertNotNull(savedUserId);
        String s3Key = savedUserId.toString()+"/path/file.txt";
        s3FileServiceImpl.uploadResource("path/",savedUserId, List.of(multipartFile));
        var responseBytes = s3Client.getObject(GetObjectRequest.builder().bucket(s3properties.bucketName()).key(s3Key).build()).readAllBytes();
        Assertions.assertArrayEquals(responseBytes, bytes);
    }

    @Test
    void shouldCreateFileForOnlyOneUser() throws IOException {
        String user1 = "testuser"; Integer user1Prefix = 1;
        String user2 = "testuser2"; Integer user2Prefix = 2;
        userService.register(new RegisterUserRequest(user1, "password"));

        byte[] bytes = new byte[] {'h', 'e', 'l', 'l', 'o'};
        MultipartFile multipartFile = new MockMultipartFile("file", "file.txt", "text/plain", bytes);
        s3FileServiceImpl.uploadResource("path/",user1Prefix, List.of(multipartFile));

        userService.register(new RegisterUserRequest(user2, "password"));
        Assertions.assertEquals("file.txt", s3FileServiceImpl.getResource("path/file.txt", user1Prefix).name());
        Assertions.assertThrows(ResourceNotFoundException.class, () -> s3FileServiceImpl.getResource("path/file.txt", user2Prefix));
    }
}
