package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.api.ResourceApi;
import dev.vivim.filecloud.dto.*;
import dev.vivim.filecloud.dto.request.MoveResourceRequest;
import dev.vivim.filecloud.dto.request.PathRequest;
import dev.vivim.filecloud.dto.request.SearchResourceRequest;
import dev.vivim.filecloud.dto.response.PathResponse;
import dev.vivim.filecloud.service.FileService;
import dev.vivim.filecloud.service.impl.S3FileServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class ResourceController implements ResourceApi {
    private final FileService fileService;

    @Override
    public PathResponse getResource(PathRequest pathReq, AuthenticatedUser user) {
        log.info("Resource Request. User: {}, Path: {}", user.id(), pathReq);
        return fileService.getResource(pathReq.path(), user.id());
    }


    @Override
    public ResponseEntity<StreamingResponseBody> downloadResource(PathRequest pathReq, AuthenticatedUser user) {
        log.info("Download resource Request. User: {}, Path: {}", user.id(), pathReq.path());
        DownloadContainer container = fileService.downloadResource(pathReq.path(), user.id());
        return ResponseEntity.ok()
                .contentType(container.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(container.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(container.fileStream());
    }


    @Override
    public PathResponse moveResource(MoveResourceRequest moveReq, AuthenticatedUser user) {
        log.info("Moving resource Request. From {} to {} for User: {}", moveReq.from(), moveReq.to(), user.id());
        return fileService.moveResource(moveReq.from(), moveReq.to(), user.id());
    }


    @Override
    public void deleteResource(PathRequest pathReq, AuthenticatedUser user) {
        log.info("Delete resource Request. User: {}, Path: {}", user.id(), pathReq.path());
        fileService.deleteResource(pathReq.path(), user.id());
    }


    @Override
    public List<PathResponse> uploadResource(String path, AuthenticatedUser user, List<MultipartFile> multipartFile) throws IOException {
        log.info("Upload resource Request. User: {}, Path: {}. Uploading {} files", user.id(), path, multipartFile.size());
        return fileService.uploadResource(path, user.id(), multipartFile);
    }


    @Override
    public List<PathResponse> searchResources(SearchResourceRequest searchReq, AuthenticatedUser user) {
        log.info("Search Request: {}. User: {}", searchReq.query(), user.id());
        return fileService.searchResources(searchReq.query(), user.id());
    }
}
