package dev.vivim.filecloud.module;

import dev.vivim.filecloud.events.UserRegisteredEvent;
import dev.vivim.filecloud.infrastructure.storage.ObjectStorage;
import dev.vivim.filecloud.minio.s3.S3Properties;
import dev.vivim.filecloud.service.impl.S3FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FilesModuleTests {
    @Mock ObjectStorage objectStorage;
    S3Properties properties = new S3Properties("123", "123", "123", "123", "123", "user-%s-files");
    S3FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        fileService = new S3FileServiceImpl(properties, null, objectStorage, null, null);
    }

    @Test
    void shouldCreateEmptyDirWhenRegisterUser() {
        fileService.onUserRegistered(new UserRegisteredEvent(1));
        verify(objectStorage, times(1)).createDirectory(any());
    }
}
