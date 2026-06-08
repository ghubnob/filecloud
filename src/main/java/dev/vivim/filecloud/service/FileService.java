package dev.vivim.filecloud.service;

import dev.vivim.filecloud.dto.DownloadContainer;
import dev.vivim.filecloud.dto.FileType;
import dev.vivim.filecloud.dto.PathResponse;
import dev.vivim.filecloud.exception.DownloadException;
import dev.vivim.filecloud.exception.FileAlreadyExistsException;
import dev.vivim.filecloud.exception.InvalidPathException;
import dev.vivim.filecloud.exception.ResourceNotFoundException;
import dev.vivim.filecloud.paths.PathObject;
import dev.vivim.filecloud.paths.PathResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class FileService {
    private final S3Client s3Client;
    private final PathResolver pathResolver;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public FileService(S3Client s3Client, PathResolver pathResolver) {
        this.s3Client = s3Client;
        this.pathResolver = pathResolver;
    }

    public PathResponse getResource(String path, String username) {
        PathObject pathObj = pathResolver.resolve(path, username);
        String fullS3Key = pathObj.getFullPath();
        String resourceName = pathObj.getLastSegment();
        String folderName = pathObj.getPrefix();

        if (path.endsWith("/")) {
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName).prefix(fullS3Key).maxKeys(1).build();
            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsV2Request);

            boolean exists = response.hasContents();
            if (!exists) throw new ResourceNotFoundException("Resource '"+path+"' not found");
            return new PathResponse(folderName, resourceName, null, FileType.DIRECTORY);
        }
        else {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName).key(fullS3Key).build();
            try {
                HeadObjectResponse response = s3Client.headObject(headObjectRequest);
                return new PathResponse(folderName, resourceName, response.contentLength(), FileType.FILE);
            } catch (NoSuchKeyException ignored) { throw new ResourceNotFoundException("File '"+path+"' not found"); }
        }
    }

    public List<PathResponse> getDirectory(String path, String username) {
        PathObject pathObj = pathResolver.resolve(path, username);
        String fullS3Key = pathObj.getFullPath();
        String folderName = pathObj.getPrefix()+pathObj.getLastSegment();

        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName).prefix(fullS3Key).delimiter("/").build();
        ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsV2Request);

        boolean exists = response.hasContents() || response.hasCommonPrefixes();
        if (!exists) throw new ResourceNotFoundException("Resource '"+path+"' not found");

        List<PathResponse> result = new ArrayList<>();

        for (var prefix : response.commonPrefixes()) {
            String key = prefix.prefix();
            String name = key.split("/")[key.split("/").length-1]+"/";
            result.add(new PathResponse(folderName, name, null, FileType.DIRECTORY));
        }
        for (var content : response.contents()) {
            String key = content.key();
            if (key.equals(fullS3Key)) continue;
            String name = key.substring(key.lastIndexOf("/")+1);
            result.add(new PathResponse(folderName, name, content.size(), FileType.FILE));
        }

        return result;
    }

    public List<PathResponse> searchResources(String query, String username) {
        List<PathResponse> result = new ArrayList<>();
        Set<PathResponse> folders = new HashSet<>();
        String fullS3Key = username + "/";

        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName).prefix(fullS3Key).build();
        var paginator = s3Client.listObjectsV2Paginator(listObjectsV2Request);

        paginator.forEach(response -> {
            for (var obj : response.contents()) {
                String key = obj.key();
                if (key.equals(fullS3Key)) continue;

                String relativeKey = key.substring(fullS3Key.length());
                int lastSlash = relativeKey.lastIndexOf("/");

                String resourceName = relativeKey.substring(lastSlash+1);
                String folderName = lastSlash<0 ? "" : relativeKey.substring(0,lastSlash+1);

                if (!key.endsWith("/") && resourceName.toLowerCase().contains(query.toLowerCase()))
                    result.add(new PathResponse(folderName, resourceName, obj.size(), FileType.FILE));

                if (folderName.toLowerCase().contains(query.toLowerCase())){
                    String[] parts = folderName.split("/");
                    if (parts.length < 1) continue;

                    StringBuilder currentFolderPath = new StringBuilder();
                    for (String part : parts) {
                        String parent = currentFolderPath.toString();
                        currentFolderPath.append(part).append("/");

                        if (part.toLowerCase().contains(query.toLowerCase()))
                            folders.add(new PathResponse(parent, part + "/", null, FileType.DIRECTORY));
                    }
                }
            }
        });

        result.addAll(folders);
        return result;
    }

    public List<PathResponse> uploadResource(String path, String username, List<MultipartFile> files) throws IOException {
        PathObject pathObj = pathResolver.resolve(path, username);
        String s3Key = pathObj.getFullPath();
        String folderName = pathObj.getPrefix()+pathObj.getLastSegment();

        List<PathResponse> response = new ArrayList<>();
        for (MultipartFile file : files) {
            String currentS3Key = s3Key + (s3Key.endsWith("/") ? "" : "/") + file.getOriginalFilename();

            try {
                s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(currentS3Key).build());
                throw new FileAlreadyExistsException("File '"+file.getOriginalFilename()+"' already exists!");
            } catch (NoSuchKeyException ignored) {}

            try (InputStream is = file.getInputStream()) {
                s3Client.putObject(b -> b.bucket(bucketName).key(currentS3Key).contentType(file.getContentType()),
                        RequestBody.fromInputStream(is, file.getSize()));
                log.debug("Gave key '{}' to uploaded file '{}'\n", currentS3Key, file.getOriginalFilename());
                response.add(new PathResponse(folderName, file.getOriginalFilename(), file.getSize(), FileType.FILE));
            }
        }

        return response;
    }

    public PathResponse moveResource(String from, String to, String username) {
        PathObject pathObjFrom = pathResolver.resolve(from, username);
        PathObject pathObjTo = pathResolver.resolve(to, username);

        if (pathObjFrom.isDirectory() != pathObjTo.isDirectory())
            throw new InvalidPathException("Invalid path on moving/renaming files!");

        String fullS3KeyFrom = pathObjFrom.getFullPath();
        String fullS3KeyTo = pathObjTo.getFullPath();

        String resourceName = pathObjFrom.getLastSegment();
        String folderName = pathObjFrom.getPrefix();

        if (pathObjFrom.isDirectory()) {
            var tryResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketName).prefix(fullS3KeyTo).maxKeys(1).build());
            if (!tryResponse.contents().isEmpty())
                throw new FileAlreadyExistsException("Folder '"+resourceName+"' already exists!");

            var paginator = s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
                    .bucket(bucketName).prefix(fullS3KeyFrom).build());

            paginator.forEach(response -> {
                for (S3Object obj : response.contents()) {
                    s3Client.copyObject(CopyObjectRequest.builder()
                            .sourceBucket(bucketName)
                            .sourceKey(obj.key())
                            .destinationBucket(bucketName)
                            .destinationKey(fullS3KeyTo + obj.key().substring(fullS3KeyFrom.length()))
                            .build());
                    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(obj.key()).build());
                }
            });

            return new PathResponse(
                    folderName,
                    resourceName,
                    null,
                    FileType.DIRECTORY);
        }
        else {
            HeadObjectResponse fromResponse = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(fullS3KeyFrom).build());
            try {
                s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(fullS3KeyTo).build());
                throw new FileAlreadyExistsException("File '"+resourceName+"' already exists!");
            } catch (NoSuchKeyException ignored) {}

            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(fullS3KeyFrom)
                    .destinationBucket(bucketName)
                    .destinationKey(fullS3KeyTo)
                    .build());

            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(fullS3KeyFrom).build());

            return new PathResponse(
                    folderName,
                    resourceName,
                    fromResponse.contentLength(),
                    FileType.FILE);
        }
    }

    public void deleteResource(String path, String username) {
        PathObject pathObj = pathResolver.resolve(path, username);
        String fullS3Key = pathObj.getFullPath();

        if (pathObj.isDirectory()) {
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName).prefix(fullS3Key).build();
            var paginator =  s3Client.listObjectsV2Paginator(listObjectsV2Request);
            paginator.forEach(response -> {
                boolean exists = response.hasContents();
                if (!exists) throw new ResourceNotFoundException("Folder '"+path+"' not found!");

                response.contents().forEach(s3Object -> s3Client.deleteObject(
                        DeleteObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build()));});
        }
        else {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName).key(fullS3Key).build();
            try {
                s3Client.headObject(headObjectRequest);
            } catch (NoSuchKeyException ignored) { throw new ResourceNotFoundException("File '"+path+"' not found!"); }

            DeleteObjectRequest delObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName).key(fullS3Key).build();
            s3Client.deleteObject(delObjectRequest);
        }
    }

    public DownloadContainer downloadResource(String path, String username) {
        PathObject pathObj = pathResolver.resolve(path, username);
        String fullS3Key = pathObj.getFullPath();
        String fileName = pathObj.getLastSegment();

        if (!pathObj.isDirectory()) {
            HeadObjectResponse head;
            try {
                head = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(fullS3Key).build());
            } catch (NoSuchKeyException ignored) { throw new ResourceNotFoundException("File '"+path+"' not found!"); }

            MediaType media = MediaType.parseMediaType(head.contentType());

            return DownloadContainer.file(out -> {
                try (var obj = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(fullS3Key).build())) {
                    obj.transferTo(out);
                }
                catch (IOException e) { throw new DownloadException("Error downloading file!"); }
            }, fileName, media);
        }

        ListObjectsV2Response check = s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).prefix(fullS3Key).maxKeys(1).build());
        if (!check.hasContents()) throw new ResourceNotFoundException("Folder "+path+" not found!");

        return DownloadContainer.folder(out -> {
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName).prefix(fullS3Key).build();
            ListObjectsV2Iterable iterable = s3Client.listObjectsV2Paginator(listObjectsV2Request);

            try (ZipOutputStream zos = new ZipOutputStream(out)) {
                for (ListObjectsV2Response response : iterable) {
                    for (S3Object s3Object : response.contents()) {
                        String relativePath = s3Object.key().substring(fullS3Key.length());
                        if (relativePath.isEmpty()) continue;

                        zos.putNextEntry(new ZipEntry(relativePath));
                        if (!s3Object.key().endsWith("/")) {
                            try (var obj = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build())) {
                                obj.transferTo(zos);
                            }
                        }

                        zos.closeEntry();
                    }
                }
            } catch (IOException e) {
                throw new DownloadException("Error downloading files!");
            }
        }, fileName+".zip");

    }

    public PathResponse createDirectory(String path, String username) {
        PathObject pathObj = pathResolver.resolve(path, username);
        String fullS3Key = pathObj.getFullPath();
        String folderName = pathObj.getLastSegment();
        String parentName = pathObj.getPrefix();

        if (!pathObj.isDirectory()) throw new InvalidPathException("Invalid path to new directory!");

        var tryResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName).prefix(fullS3Key).maxKeys(1).build());
        if (!tryResponse.contents().isEmpty()) throw new FileAlreadyExistsException("Directory already exists!");

        var parentResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName).prefix(username+"/"+parentName).maxKeys(1).build());
        if (parentResponse.contents().isEmpty() && !parentName.isEmpty()) throw new ResourceNotFoundException("Parent directory not exists!");

        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(fullS3Key).build(), RequestBody.empty());

        return new PathResponse(folderName, parentName, null, FileType.DIRECTORY);
    }


    public void createRootFolderOnRegistration(String username) {
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(username+"/").build(), RequestBody.empty());
    }
}
