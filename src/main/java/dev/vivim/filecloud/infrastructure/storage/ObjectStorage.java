package dev.vivim.filecloud.infrastructure.storage;

import dev.vivim.filecloud.dto.storage.StorageDirectoryContent;
import dev.vivim.filecloud.dto.storage.StorageFileSummary;
import dev.vivim.filecloud.dto.storage.StorageObjectMetadata;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface ObjectStorage {
    void putObject(String key, InputStream content, long size, String contentType);
    boolean doesObjectExists(String key);
    boolean doesDirectoryExists(String key);
    void deleteObject(String key);
    void deleteDirectory(String key);
    StorageObjectMetadata getObjectMetadata(String key);
    StorageDirectoryContent getDirectoryContent(String key);
    void createDirectory(String key);
    List<StorageFileSummary> getAllObjectsByPrefix(String prefix);
    void copyResource(String keyFrom, String keyTo);
    InputStream getObjectContent(String key);
}
