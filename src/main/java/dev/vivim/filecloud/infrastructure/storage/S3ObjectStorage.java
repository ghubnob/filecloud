package dev.vivim.filecloud.infrastructure.storage;

import dev.vivim.filecloud.dto.storage.StorageDirectoryContent;
import dev.vivim.filecloud.dto.storage.StorageFileSummary;
import dev.vivim.filecloud.dto.storage.StorageObjectMetadata;
import dev.vivim.filecloud.exception.DownloadException;
import dev.vivim.filecloud.exception.ResourceNotFoundException;
import dev.vivim.filecloud.minio.s3.S3Properties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class S3ObjectStorage implements ObjectStorage {
    S3Client s3Client;
    S3Properties s3Properties;

    @Override
    public void putObject(String s3Key, InputStream content, long size, String contentType) {
        s3Client.putObject(b -> b
                .bucket(s3Properties.bucketName())
                .key(s3Key)
                .contentType(contentType)
                .build(), RequestBody.fromInputStream(content, size));
    }

    @Override
    public boolean doesObjectExists(String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(s3Properties.bucketName()).key(s3Key).build());
            return true;
        } catch (NoSuchKeyException ignored) { return false; }
    }

    @Override
    public void deleteObject(String s3Key) {
        s3Client.deleteObject(b -> b
                .bucket(s3Properties.bucketName())
                .key(s3Key));
    }

    @Override
    public void deleteDirectory(String s3Key) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(s3Properties.bucketName()).prefix(s3Key).build();
        var paginator =  s3Client.listObjectsV2Paginator(listObjectsV2Request);
        paginator.forEach(response -> {
            if(!response.hasContents()) return;
            response.contents().forEach(s3Object -> s3Client.deleteObject(
                    DeleteObjectRequest.builder().bucket(s3Properties.bucketName()).key(s3Object.key()).build()));
        });
    }

    @Override
    public StorageObjectMetadata getObjectMetadata(String s3Key) {
        if (s3Key.endsWith("/")) {
            ListObjectsV2Response response = s3Client.listObjectsV2(b -> b
                    .bucket(s3Properties.bucketName()).prefix(s3Key).maxKeys(1));
            if (!response.hasContents()) throw new ResourceNotFoundException("Resource not found");
            return new StorageObjectMetadata(null, true, "application/zip");
        }
        try {
            HeadObjectResponse response = s3Client.headObject(b -> b.bucket(s3Properties.bucketName()).key(s3Key).build());
            return new StorageObjectMetadata(response.contentLength(), false, response.contentType());
        } catch (NoSuchKeyException ignored) { throw new ResourceNotFoundException("Resource not found"); }
    }

//    @Override
//    public StorageDirectoryContent getDirectoryContent(String s3Key) {
//        ListObjectsV2Response response = s3Client.listObjectsV2(b -> b.bucket(s3Properties.bucketName()).prefix(s3Key).delimiter("/"));
//
//        boolean exists = response.hasContents() || response.hasCommonPrefixes();
//        if (!exists) throw new ResourceNotFoundException("Resource not found");
//
//        List<String> prefixes = response.commonPrefixes().stream()
//                .map(CommonPrefix::prefix).toList();
//
//        List<StorageFileSummary> files = response.contents().stream()
//                .map(obj -> new StorageFileSummary(obj.key(), obj.size())).toList();
//
//        return new StorageDirectoryContent(prefixes, files);
//    }

    @Override
    public void createDirectory(String s3Key) {
        s3Client.putObject(PutObjectRequest.builder().bucket(s3Properties.bucketName()).key(s3Key).build(), RequestBody.empty());
    }

    @Override
    public boolean doesDirectoryExists(String prefix) {
        return s3Client.listObjectsV2(b -> b
                .bucket(s3Properties.bucketName()).prefix(prefix).maxKeys(1)).hasContents();
    }

    @Override
    public List<StorageFileSummary> getAllObjectsByPrefix(String prefix) {
        var paginator = s3Client.listObjectsV2Paginator(b -> b.bucket(s3Properties.bucketName()).prefix(prefix));

        List<StorageFileSummary> files = new ArrayList<>();

        paginator.forEach(response -> {
            for (var obj : response.contents()) {
                files.add(new StorageFileSummary(obj.key(), obj.size()));
            }
        });

        return files;
    }

    @Override
    public void copyResource(String keyFrom, String keyTo) {
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(s3Properties.bucketName())
                .sourceKey(keyFrom)
                .destinationBucket(s3Properties.bucketName())
                .destinationKey(keyTo)
                .build());
    }

    @Override
    public InputStream getObjectContent(String s3Key) {
        try {
            return s3Client.getObject(b -> b.bucket(s3Properties.bucketName()).key(s3Key));
        } catch (NoSuchKeyException ignored) { throw new DownloadException("Resource not found!"); }
    }
}
