package dev.vivim.filecloud.service.impl;

import dev.vivim.filecloud.dto.DownloadContainer;
import dev.vivim.filecloud.dto.FileType;
import dev.vivim.filecloud.dto.UserStorageRoot;
import dev.vivim.filecloud.dto.response.PathResponse;
import dev.vivim.filecloud.events.UserRegisteredEvent;
import dev.vivim.filecloud.exception.DownloadException;
import dev.vivim.filecloud.exception.ResourceAlreadyExistsException;
import dev.vivim.filecloud.exception.InvalidPathException;
import dev.vivim.filecloud.exception.ResourceNotFoundException;
import dev.vivim.filecloud.infrastructure.storage.ObjectStorage;
import dev.vivim.filecloud.minio.s3.S3Properties;
import dev.vivim.filecloud.infrastructure.paths.PathObject;
import dev.vivim.filecloud.infrastructure.paths.PathResolver;
import dev.vivim.filecloud.model.ResourceEntity;
import dev.vivim.filecloud.repository.ResourceMetadataRepository;
import dev.vivim.filecloud.service.FileService;
import dev.vivim.filecloud.util.ZipArchiver;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.util.*;

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
    ResourceMetadataRepository resourceRepository;

    @Override
    public PathResponse getResource(String path, Integer parentPrefix) {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, parentPrefix));
        String resourceName = pathObj.getLastSegment();
        String folderName = pathObj.getPrefix();

        ResourceEntity resource = resourceRepository
                .findByUserIdAndPathAndName(parentPrefix, folderName, resourceName)
                .orElseThrow(() -> new ResourceNotFoundException("Resource " + resourceName + " not found"));

        return PathResponse.from(resource);
    }

    @Override
    public List<PathResponse> getDirectory(String path, Integer userId) {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, userId));
        String relativeFolder = pathObj.getPrefix() + pathObj.getLastSegment();
        if (!relativeFolder.isBlank() && !relativeFolder.endsWith("/")) relativeFolder += "/";

        if (!relativeFolder.isBlank()) {
            String folderName = relativeFolder.substring(0, relativeFolder.length() - 1);
            int idx = folderName.lastIndexOf('/');

            String parentPath = idx < 0 ? "" : folderName.substring(0, idx + 1);
            String name = idx < 0 ? folderName : folderName.substring(idx + 1);

            resourceRepository
                    .findByUserIdAndPathAndName(userId, parentPath, name)
                    .orElseThrow(() -> new ResourceNotFoundException("Directory '" + name + "' not found"));
        }

        return resourceRepository
                .findAllByUserIdAndPathOrderByResourceTypeDescNameAsc(userId, relativeFolder)
                .stream()
                .map(PathResponse::from)
                .toList();
    }

    @Override
    public List<PathResponse> searchResources(String query, Integer parentPrefix) {
        return resourceRepository.searchByUserIdAndName(parentPrefix, query.trim()).stream()
                .map(PathResponse::from).toList();
    }

    @Transactional
    @Override
    public List<PathResponse> uploadResource(String path, Integer userId, List<MultipartFile> files) throws IOException {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, userId));
        String s3Key = pathObj.getFullPath();
        String folderName = pathObj.getPrefix()+pathObj.getLastSegment();

        if (!folderName.isBlank() && !folderName.endsWith("/")) folderName += "/";
        log.info("[upload] for user {} - prefix: {}, last segment: {}", userId, pathObj.getPrefix(), pathObj.getLastSegment());
        ensureDirectoryMetadata(userId, folderName);

        List<PathResponse> response = new ArrayList<>();
        for (MultipartFile file : files) {
            String currentFilename = file.getOriginalFilename();
            if (currentFilename == null || currentFilename.isBlank())
                throw new InvalidPathException("File name is null!");
            PathObject relativeFile = pathResolver.resolve(currentFilename, new UserStorageRoot(""));
            String currentS3Key = s3Key + (s3Key.endsWith("/") ? "" : "/") +
                    relativeFile.getPrefix() + relativeFile.getLastSegment();

            if (objectStorage.doesObjectExists(currentS3Key))
                throw new ResourceAlreadyExistsException("File '" + currentFilename + "' already exists!");

            String finalPath = folderName + relativeFile.getPrefix();
            ensureDirectoryMetadata(userId, finalPath);
            try (InputStream is = file.getInputStream()) {
                objectStorage.putObject(currentS3Key, is, file.getSize(), file.getContentType());
                log.debug("Gave key '{}' to uploaded file '{}'\n", currentS3Key, currentFilename);

                try {
                    ResourceEntity resource = ResourceEntity.createOf(userId,
                            finalPath, relativeFile.getLastSegment(),
                            file.getSize(), FileType.FILE);
                    resourceRepository.save(resource);
                } catch (DataIntegrityViolationException de) {
                    objectStorage.deleteObject(currentS3Key);
                    throw new ResourceAlreadyExistsException("File '" + currentFilename + "' already exists!");
                }
                catch (Exception e) {
                    try { objectStorage.deleteObject(currentS3Key); }
                    catch (Exception cleanup) { log.error("Cleanup failed for key {}", currentS3Key, cleanup); }
                    throw e;
                }

                response.add(new PathResponse(finalPath,
                        relativeFile.getLastSegment(),
                        file.getSize(),
                        FileType.FILE));

            }
        }

        return response;
    }

    @Transactional
    @Override
    public PathResponse moveResource(String from, String to, Integer userId) {
        var root = UserStorageRoot.forUser(s3Properties, userId);
        PathObject pathObjFrom = pathResolver.resolve(from, root);
        PathObject pathObjTo = pathResolver.resolve(to, root);

        if (pathObjFrom.isDirectory() != pathObjTo.isDirectory())
            throw new InvalidPathException("Invalid path on moving/renaming files!");

        String fullS3KeyFrom = pathObjFrom.getFullPath();
        String fullS3KeyTo = pathObjTo.getFullPath();

        String fromName = pathObjFrom.getLastSegment();
        String toName = pathObjTo.getLastSegment();

        String fromPrefix = pathObjFrom.getPrefix()+fromName + (fromName.endsWith("/") ? "" : "/");

        if (pathObjFrom.isDirectory()) {
            if (objectStorage.doesDirectoryExists(fullS3KeyTo))
                throw new ResourceAlreadyExistsException("Directory '"+toName+"' already exists!");

            List<String> sourceKeys = new ArrayList<>();
            List<String> copiedKeys = new ArrayList<>();
            List<ResourceEntity> toSave = new ArrayList<>();
            List<ResourceEntity> toDelete = new ArrayList<>();
            try {
                String fromFolderName = fromName.endsWith("/") ? fromName.substring(0, fromName.length()-1) : fromName;
                String toFolderName = toName.endsWith("/") ? toName.substring(0, toName.length()-1) : toName;

                ResourceEntity folder = resourceRepository
                        .findByUserIdAndPathAndName(userId, pathObjFrom.getPrefix(), fromFolderName)
                        .orElseThrow(() -> new ResourceNotFoundException("Directory '"+fromName+"' not found!"));

                toDelete.add(folder);
                toSave.add(ResourceEntity.createOf(
                        folder.getUserId(),
                        pathObjTo.getPrefix(),
                        toFolderName,
                        0L, FileType.DIRECTORY));

                String fromFolderPath = pathObjFrom.getPrefix() + fromFolderName + "/";
                String toFolderPath = pathObjTo.getPrefix() + toFolderName + "/";

                List<ResourceEntity> resources = resourceRepository.findAllByUserIdAndPathStartingWith(userId, fromPrefix);
                log.info("[move] for user {} moving {} files from folder {} to folder {}",
                        userId, resources.size(), fromFolderPath, toFolderPath);

                resources.forEach(res -> {
                    String sourceKey = root.value() + "/" + res.getPath() + res.getName();
                    String newPath = res.getPath().replace(fromFolderPath, toFolderPath);
                    String destinationKey = root.value() + "/" + newPath + res.getName();

                    objectStorage.copyResource(sourceKey, destinationKey);
                    copiedKeys.add(destinationKey);
                    sourceKeys.add(sourceKey);

                    toSave.add(ResourceEntity.createOf(res.getUserId(),
                            newPath,
                            res.getName(),
                            res.getSize(),
                            res.getResourceType()));
                    toDelete.add(res);
                });
                resourceRepository.saveAll(toSave);
                resourceRepository.deleteAll(toDelete);
                for (String key : sourceKeys) {
                    try { objectStorage.deleteObject(key); }
                    catch (Exception ex) { log.error("Deleting source files while copying failed for key {}", key); throw ex; }
                }

                return new PathResponse(pathObjTo.getPrefix(), toName, null, FileType.DIRECTORY);
            }
            catch (Exception e) {
                for (String key : copiedKeys) {
                    try { objectStorage.deleteObject(key); }
                    catch (Exception cleanup) { log.error("Cleanup failed on key {}", key, cleanup); }
                }
                throw e;
            }
        }
        else {
            var fromMetadata = objectStorage.getObjectMetadata(fullS3KeyFrom);
            if (objectStorage.doesObjectExists(fullS3KeyTo))
                throw new ResourceAlreadyExistsException("File '"+toName+"' already exists!");

            var res = resourceRepository.findByUserIdAndPathAndName(userId, pathObjFrom.getPrefix(), fromName)
                    .orElseThrow(() -> new ResourceNotFoundException("File '"+fromName+"' not found!"));

            objectStorage.copyResource(fullS3KeyFrom, fullS3KeyTo);
            try {
                resourceRepository.save(ResourceEntity.createOf(res.getUserId(),
                        pathObjTo.getPrefix(),
                        toName,
                        res.getSize(),
                        res.getResourceType()));
                resourceRepository.delete(res);
            } catch (Exception e) {
                objectStorage.deleteObject(fullS3KeyTo);
                throw e;
            }
            objectStorage.deleteObject(fullS3KeyFrom);

            return new PathResponse(
                    pathObjTo.getPrefix(),
                    toName,
                    fromMetadata.size(),
                    FileType.FILE);
        }
    }

    @Transactional
    @Override
    public void deleteResource(String path, Integer userId) {
        PathObject pathObj = pathResolver.resolve(path, UserStorageRoot.forUser(s3Properties, userId));
        String fullS3Key = pathObj.getFullPath();
        String relativePath = pathObj.getPrefix()+pathObj.getLastSegment();

        if (pathObj.isDirectory()) {
            var toDelete = resourceRepository.findAllByUserIdAndPathStartingWith(userId, relativePath);
            String dirName = pathObj.getLastSegment().endsWith("/")
                    ? pathObj.getLastSegment().substring(0, pathObj.getLastSegment().length()-1)
                    : pathObj.getLastSegment();

            objectStorage.deleteDirectory(fullS3Key);
            resourceRepository.deleteAll(toDelete);
            resourceRepository.findByUserIdAndPathAndName(userId, pathObj.getPrefix(), dirName)
                    .ifPresent(resourceRepository::delete);
        }
        else {
            if (!objectStorage.doesObjectExists(fullS3Key)) {
                throw new ResourceNotFoundException("File '"+pathObj.getLastSegment()+"' does not exist!");
            }
            objectStorage.deleteObject(fullS3Key);
            resourceRepository.deleteByUserIdAndPathAndName(userId, pathObj.getPrefix(), pathObj.getLastSegment());
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

    @Transactional
    @Override
    public PathResponse createDirectory(String path, Integer parentPrefix) {
        var root = UserStorageRoot.forUser(s3Properties, parentPrefix);
        PathObject pathObj = pathResolver.resolve(path, root);
        String fullS3Key = pathObj.getFullPath();
        String folderName = pathObj.getLastSegment();
        String parentName = pathObj.getPrefix();

        if (!pathObj.isDirectory())
            throw new InvalidPathException("Invalid path to new directory!");

        if (objectStorage.doesDirectoryExists(fullS3Key))
            throw new ResourceAlreadyExistsException("Directory already exists!");

        if (!parentName.isBlank() && !objectStorage.doesDirectoryExists(root.value()+"/"+parentName))
            throw new ResourceNotFoundException("Parent directory not exists!");

        objectStorage.createDirectory(fullS3Key);
        try {
            String name = folderName.endsWith("/") ? folderName.substring(0, folderName.length()-1) : folderName;
            resourceRepository.save(ResourceEntity.createOf(parentPrefix, parentName, name, 0L, FileType.DIRECTORY));

            return new PathResponse(parentName, folderName, null, FileType.DIRECTORY);
        } catch (Exception e) {
            try {
                objectStorage.deleteDirectory(fullS3Key);
            } catch (Exception cleanup) { log.error("Cleanup failed for key {}", fullS3Key, cleanup); }
            throw e;
        }
    }


    @TransactionalEventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        String root = UserStorageRoot.forUser(s3Properties, event.userId()).value()+"/";
        objectStorage.createDirectory(root);
    }

    private void ensureDirectoryMetadata(Integer userId, String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) return;

        String normalized = directoryPath;
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.isBlank()) return;
        log.info("Ensuring directory metadata for user {}, normalized: {}", userId, normalized);

        String currentPath = "";
        for (String segment : normalized.split("/")) {
            if (resourceRepository.findByUserIdAndPathAndName(userId, currentPath, segment).isEmpty()) {
                resourceRepository.save(
                        ResourceEntity.createOf(
                                userId,
                                currentPath,
                                segment,
                                0L,
                                FileType.DIRECTORY));
            }
            currentPath += segment + "/";
        }
    }
}
