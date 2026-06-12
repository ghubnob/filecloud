 package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.dto.PathResponse;
import dev.vivim.filecloud.dto.annotation.ResourceOperationResponses;
import dev.vivim.filecloud.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@RestController
@RequestMapping("/api/directory")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Directories", description = "Управление папками файлового хранилища")
public class DirectoryController {
    private final FileService fileService;

    @GetMapping
    @Operation(summary = "Получение информации о содержимом папки")
    @ApiResponse(responseCode = "200", description = "Информация о папке успешно получена")
    @ResourceOperationResponses
    public List<PathResponse> getDirectory(@RequestParam String path, @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Directory Request: {}/{}", user.id(), path);
        return fileService.getDirectory(path, user.id());
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создание пустой папки")
    @ApiResponse(responseCode = "201", description = "Пустая папка создана")
    @ResourceOperationResponses
    @ApiResponse(responseCode = "409", description = "Папка уже существует")
    public PathResponse createDirectory(@RequestParam String path, @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Create empty directory Request: {}/{}", user.id(), path);
        return fileService.createDirectory(path, user.id());
    }
}
