package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.controller.api.ResourceApi;
import dev.vivim.filecloud.dto.*;
import dev.vivim.filecloud.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ResourceController implements ResourceApi {
    private final FileService fileService;

    @Override
    public PathResponse getResource(String path, AuthenticatedUser user) {
        log.info("Resource Request: {}/{}", user.id(), path);
        return fileService.getResource(path, user.id());
    }


    @Override
    public ResponseEntity<StreamingResponseBody> downloadResource(String path, AuthenticatedUser user) {
        log.info("Download resource Request: {}/{}", user.id(), path);
        DownloadContainer container = fileService.downloadResource(path, user.id());
        return ResponseEntity.ok()
                .contentType(container.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(container.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(container.fileStream());
    }


    @Override
    public PathResponse moveResource(String from, String to, AuthenticatedUser user) {
        log.info("Moving resource Request: from {}/{} to {}/{}", user.id(), from, user.id(), to);
        return fileService.moveResource(from, to, user.id());
    }


    @Override
    public void deleteResource(String path, AuthenticatedUser user) {
        log.info("Delete resource Request: {}/{}", user.id(), path);
        fileService.deleteResource(path, user.id());
    }


    @Override
    public List<PathResponse> uploadResource(String path, AuthenticatedUser user, List<MultipartFile> multipartFile) throws IOException {
        log.info("Upload resource Request: {}/{}, uploading {} files", user.id(), path, multipartFile.size());
        return fileService.uploadResource(path, user.id(), multipartFile);
    }


    @Override
    public List<PathResponse> searchResources(String query, AuthenticatedUser user) {
        log.info("Search Request: {}. User: {}", query, user.id());
        return fileService.searchResources(query, user.id());
    }
}
