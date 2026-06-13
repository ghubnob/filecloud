 package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.controller.api.DirectoryApi;
import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.dto.PathResponse;
import dev.vivim.filecloud.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class DirectoryController implements DirectoryApi {
    private final FileService fileService;

    @Override
    public List<PathResponse> getDirectory(String path, AuthenticatedUser user) {
        log.info("Directory Request: {}/{}", user.id(), path);
        return fileService.getDirectory(path, user.id());
    }


    @Override
    public PathResponse createDirectory(String path, AuthenticatedUser user) {
        log.info("Create empty directory Request: {}/{}", user.id(), path);
        return fileService.createDirectory(path, user.id());
    }
}
