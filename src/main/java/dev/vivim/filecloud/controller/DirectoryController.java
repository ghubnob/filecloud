 package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.controller.api.DirectoryApi;
import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.dto.request.PathRequest;
import dev.vivim.filecloud.dto.response.PathResponse;
import dev.vivim.filecloud.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class DirectoryController implements DirectoryApi {
    private final FileService fileService;

    @Override
    public List<PathResponse> getDirectory(String path, AuthenticatedUser user) {
        log.info("Directory Request. User: {}, Path: {}", user.id(), path);
        return fileService.getDirectory(path, user.id());
    }


    @Override
    public PathResponse createDirectory(PathRequest pathReq, AuthenticatedUser user) {
        log.info("Create empty directory Request. User: {}, Path: {}", user.id(), pathReq.path());
        return fileService.createDirectory(pathReq.path(), user.id());
    }
}
