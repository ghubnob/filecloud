package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.dto.*;
import dev.vivim.filecloud.dto.annotation.ResourceOperationResponses;
import dev.vivim.filecloud.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequestMapping("/api/resource")
@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Управление ресурсами файлового хранилища")
public class ResourceController {
    private final FileService fileService;

    @GetMapping
    @Operation(summary = "Получение информации о ресурсе")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно получен")
    @ResourceOperationResponses
    public PathResponse getResource(@RequestParam String path, @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Resource Request: {}/{}", user.id(), path);
        return fileService.getResource(path, user.id());
    }


    @GetMapping("/download")
    @Operation(summary = "Скачивание ресурса")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно начал скачиваться")
    @ResourceOperationResponses
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam String path,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Download resource Request: {}/{}", user.id(), path);
        DownloadContainer container = fileService.downloadResource(path, user.id());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(container.contentType());
        headers.setContentDisposition(ContentDisposition.attachment().filename(container.fileName(), StandardCharsets.UTF_8).build());

        StreamingResponseBody body = outputStream -> container.writer().accept(outputStream);
        return ResponseEntity.ok().headers(headers).body(body);
    }


    @GetMapping("/move")
    @Operation(summary = "Переименование/перемещение ресурса")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно перемещен")
    @ResourceOperationResponses
    @ApiResponse(responseCode = "409", description = "Ресурс по пути перемещения уже существует")
    public PathResponse moveResource(@RequestParam String from,
                                     @RequestParam String to,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Moving resource Request: from {}/{} to {}/{}", user.id(), from, user.id(), to);
        return fileService.moveResource(from, to, user.id());
    }


    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление ресурса")
    @ApiResponse(responseCode = "204", description = "Ресурс успешно удален")
    @ResourceOperationResponses
    public void deleteResource(@RequestParam String path, @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Delete resource Request: {}/{}", user.id(), path);
        fileService.deleteResource(path, user.id());
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Загрузка ресурса на хранилище")
    @ApiResponse(responseCode = "201", description = "Ресурс успешно начал загружаться")
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    @ApiResponse(responseCode = "409", description = "Файл уже существует")
    public List<PathResponse> uploadResource(@RequestParam String path,
                                             @AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestPart("object") List<MultipartFile> multipartFile) throws IOException {
        log.info("Upload resource Request: {}/{}, uploading {} files", user.id(), path, multipartFile.size());
        return fileService.uploadResource(path, user.id(), multipartFile);
    }


    @GetMapping("/search")
    @Operation(summary = "Поиск ресурсов")
    @ApiResponse(responseCode = "200", description = "Поиск ресурсов успешно выполнен")
    @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий поисковый запрос")
    @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    public List<PathResponse> searchResources(@RequestParam String query, @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Search Request: {}. User: {}", query, user.id());
        return fileService.searchResources(query, user.id());
    }
}
