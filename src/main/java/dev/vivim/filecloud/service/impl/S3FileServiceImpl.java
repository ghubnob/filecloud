package dev.vivim.filecloud.service.impl;

import dev.vivim.filecloud.dto.DownloadContainer;
import dev.vivim.filecloud.dto.FileType;
import dev.vivim.filecloud.dto.UserStorageRoot;
import dev.vivim.filecloud.dto.response.PathResponse;
import dev.vivim.filecloud.dto.storage.StorageDirectoryContent;
import dev.vivim.filecloud.dto.storage.StorageFileSummary;
import dev.vivim.filecloud.events.UserRegisteredEvent;
import dev.vivim.filecloud.exception.DownloadException;
import dev.vivim.filecloud.exception.ResourceAlreadyExistsException;
import dev.vivim.filecloud.exception.InvalidPathException;
import dev.vivim.filecloud.exception.ResourceNotFoundException;
import dev.vivim.filecloud.infrastructure.storage.ObjectStorage;
import dev.vivim.filecloud.minio.s3.S3Properties;
import dev.vivim.filecloud.infrastructure.paths.PathObject;
import dev.vivim.filecloud.infrastructure.paths.PathResolver;
import dev.vivim.filecloud.service.FileService;
import dev.vivim.filecloud.util.ZipArchiver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(name = "storage.provider", havingValue = "aws")
public class S3FileServiceImpl implements FileService {
    S3Properties s3Properties;
    PathResolver pathResolver;
    ObjectStorage objectStorage;
    ZipArchiver zipArchiver;

    public PathResponse getResource(String path, Integer parentPrefix) {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, parentPrefix));
        String fullS3Key = pathObj.getFullPath();
        String resourceName = pathObj.getLastSegment();
        String folderName = pathObj.getPrefix();

        var storageResponse = objectStorage.getObjectMetadata(fullS3Key);
        FileType fileType = storageResponse.isDirectory() ? FileType.DIRECTORY : FileType.FILE;
        return new PathResponse(folderName, resourceName, storageResponse.size(), fileType);
    }

    public List<PathResponse> getDirectory(String path, Integer parentPrefix) {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, parentPrefix));
        String fullS3Key = pathObj.getFullPath();
        String folderName = pathObj.getPrefix()+pathObj.getLastSegment();

        StorageDirectoryContent dirContent = objectStorage.getDirectoryContent(fullS3Key);

        List<PathResponse> result = new ArrayList<>();

        for (String key : dirContent.prefixes()) {
            String name = key.split("/")[key.split("/").length-1]+"/";
            result.add(new PathResponse(folderName, name, null, FileType.DIRECTORY));
        }
        for (var content : dirContent.files()) {
            String key = content.key();
            if (key.equals(fullS3Key)) continue;
            String name = key.substring(key.lastIndexOf("/")+1);
            result.add(new PathResponse(folderName, name, content.size(), FileType.FILE));
        }

        return result;
    }

    public List<PathResponse> searchResources(String query, Integer parentPrefix) {
        var root = UserStorageRoot.forUser(s3Properties, parentPrefix);
        List<PathResponse> result = new ArrayList<>();
        Set<PathResponse> folders = new HashSet<>();
        String fullS3Key = root.value() + "/";

        List<StorageFileSummary> files = objectStorage.getAllObjectsByPrefix(fullS3Key);
        for (var file : files) {
            if (file.key().equals(fullS3Key)) continue;
            String relativeKey = file.key().substring(fullS3Key.length());
            int lastSlash = relativeKey.lastIndexOf("/");

            String resourceName = relativeKey.substring(lastSlash+1);
            String folderName = lastSlash<0 ? "" : relativeKey.substring(0,lastSlash+1);

            if (!file.key().endsWith("/") && resourceName.toLowerCase().contains(query.toLowerCase()))
                result.add(new PathResponse(folderName, resourceName, file.size(), FileType.FILE));

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

        result.addAll(folders);
        return result;
    }

    public List<PathResponse> uploadResource(String path, Integer parentPrefix, List<MultipartFile> files) throws IOException {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, parentPrefix));
        String s3Key = pathObj.getFullPath();
        String folderName = pathObj.getPrefix()+pathObj.getLastSegment();

        List<PathResponse> response = new ArrayList<>();
        for (MultipartFile file : files) {
            PathObject relativeFile = pathResolver.resolve(file.getOriginalFilename(), new UserStorageRoot(""));
            String currentS3Key = s3Key + (s3Key.endsWith("/") ? "" : "/") + relativeFile.getFullPath();

            if (objectStorage.doesObjectExists(currentS3Key))
                throw new ResourceAlreadyExistsException("File '"+file.getOriginalFilename()+"' already exists!");

            try (InputStream is = file.getInputStream()) {
                objectStorage.putObject(currentS3Key, is, file.getSize(), file.getContentType());
                log.debug("Gave key '{}' to uploaded file '{}'\n", currentS3Key, file.getOriginalFilename());
                response.add(new PathResponse(folderName, relativeFile.getLastSegment(), file.getSize(), FileType.FILE));
            }
        }

        return response;
    }

    public PathResponse moveResource(String from, String to, Integer parentPrefix) {
        var root = UserStorageRoot.forUser(s3Properties, parentPrefix);
        PathObject pathObjFrom = pathResolver.resolve(from, root);
        PathObject pathObjTo = pathResolver.resolve(to, root);

        if (pathObjFrom.isDirectory() != pathObjTo.isDirectory())
            throw new InvalidPathException("Invalid path on moving/renaming files!");

        String fullS3KeyFrom = pathObjFrom.getFullPath();
        String fullS3KeyTo = pathObjTo.getFullPath();

        String toResourceName = pathObjTo.getLastSegment();
        String toFolderName = pathObjTo.getPrefix();

        if (pathObjFrom.isDirectory()) {
            if (objectStorage.doesDirectoryExists(fullS3KeyTo))
                throw new ResourceAlreadyExistsException("Folder '"+toResourceName+"' already exists!");

            List<String> destinationKeys = new ArrayList<>();
            try {
                objectStorage.getAllObjectsByPrefix(fullS3KeyFrom).forEach(obj -> {
                    String sourceKey = obj.key();
                    String destinationKey = fullS3KeyTo + obj.key().substring(fullS3KeyTo.length());
                    objectStorage.copyResource(sourceKey, destinationKey);
                    destinationKeys.add(destinationKey);
                });
                objectStorage.getAllObjectsByPrefix(fullS3KeyFrom).forEach(obj -> {
                    objectStorage.deleteObject(obj.key());
                });
            } catch (Exception ignored) {
                destinationKeys.forEach(objectStorage::deleteObject);
            }

            return new PathResponse(
                    toFolderName,
                    toResourceName,
                    null,
                    FileType.DIRECTORY);
        }
        else {
            var fromMetadata = objectStorage.getObjectMetadata(fullS3KeyFrom);
            if (objectStorage.doesObjectExists(fullS3KeyTo))
                throw new ResourceAlreadyExistsException("File '"+toResourceName+"' already exists!");

            objectStorage.copyResource(fullS3KeyFrom, fullS3KeyTo);
            objectStorage.deleteObject(fullS3KeyFrom);

            return new PathResponse(
                    toFolderName,
                    toResourceName,
                    fromMetadata.size(),
                    FileType.FILE);
        }
    }

    @Override
    public void deleteResource(String path, Integer parentPrefix) {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, parentPrefix));
        String fullS3Key = pathObj.getFullPath();

        if (pathObj.isDirectory()) {
            objectStorage.deleteDirectory(fullS3Key);
        }
        else {
            if (!objectStorage.doesObjectExists(fullS3Key)) {
                throw new ResourceNotFoundException("File '"+pathObj.getLastSegment()+"' does not exist!");
            }
            objectStorage.deleteObject(fullS3Key);
        }
    }

    @Override
    public DownloadContainer downloadResource(String path, Integer parentPrefix) {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, parentPrefix));
        String fullS3Key = pathObj.getFullPath();
        String fileName = pathObj.getLastSegment();

        if (!pathObj.isDirectory()) {
            var metadata = objectStorage.getObjectMetadata(fullS3Key);

            MediaType media = metadata.contentType()==null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(metadata.contentType());

            return new DownloadContainer(out -> {
                try (InputStream is = objectStorage.getObjectContent(fullS3Key)) {
                    is.transferTo(out);
                }
                catch (IOException e) { throw new DownloadException("Error downloading file!"); }
            }, fileName, media);
        }

        if (!objectStorage.doesDirectoryExists(fullS3Key))
            throw new ResourceNotFoundException("Folder '"+path+"' not found!");

        return DownloadContainer.folder(out -> zipArchiver.archive(fullS3Key, out), fileName);
    }

    @Override
    public PathResponse createDirectory(String path, Integer parentPrefix) {
        var root = UserStorageRoot.forUser(s3Properties, parentPrefix);
        PathObject pathObj = pathResolver.resolve(path, root);
        String fullS3Key = pathObj.getFullPath();
        String folderName = pathObj.getLastSegment();
        String parentName = pathObj.getPrefix();

        if (!pathObj.isDirectory()) throw new InvalidPathException("Invalid path to new directory!");

        if (objectStorage.doesDirectoryExists(fullS3Key))
            throw new ResourceAlreadyExistsException("Directory already exists!");

        if (!parentName.isBlank() && !objectStorage.doesDirectoryExists(root.value()+"/"+parentName))
            throw new ResourceNotFoundException("Parent directory not exists!");

        objectStorage.createDirectory(fullS3Key);

        return new PathResponse(parentName, folderName, null, FileType.DIRECTORY);
    }


    @TransactionalEventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        String root = UserStorageRoot.forUser(s3Properties, event.userId()).value()+"/";
        objectStorage.putObject(root, InputStream.nullInputStream(), 0, "application/x-directory");
    }
}
