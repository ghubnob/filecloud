package dev.vivim.filecloud.controller.api;

import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.dto.PathResponse;
import dev.vivim.filecloud.dto.annotation.ResourceOperationResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;

@RequestMapping("/api/resource")
@Tag(name = "Resources", description = "Управление ресурсами файлового хранилища")
public interface ResourceApi {
    @GetMapping
    @Operation(summary = "Получение информации о ресурсе")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно получен")
    @ResourceOperationResponses
    PathResponse getResource(@RequestParam String path, @AuthenticationPrincipal AuthenticatedUser user);

    @GetMapping("/download")
    @Operation(summary = "Скачивание ресурса")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно начал скачиваться")
    @ResourceOperationResponses
    ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam String path,
                                                           @AuthenticationPrincipal AuthenticatedUser user);

    @GetMapping("/move")
    @Operation(summary = "Переименование/перемещение ресурса")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно перемещен")
    @ResourceOperationResponses
    @ApiResponse(responseCode = "409", description = "Ресурс по пути перемещения уже существует")
    PathResponse moveResource(@RequestParam String from,
                                     @RequestParam String to,
                                     @AuthenticationPrincipal AuthenticatedUser user);

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление ресурса")
    @ApiResponse(responseCode = "204", description = "Ресурс успешно удален")
    @ResourceOperationResponses
    void deleteResource(@RequestParam String path, @AuthenticationPrincipal AuthenticatedUser user);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Загрузка ресурса на хранилище")
    @ApiResponse(responseCode = "201", description = "Ресурс успешно начал загружаться")
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    @ApiResponse(responseCode = "409", description = "Файл уже существует")
    List<PathResponse> uploadResource(@RequestParam String path,
                                             @AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestPart("object") List<MultipartFile> multipartFile) throws IOException;

    @GetMapping("/search")
    @Operation(summary = "Поиск ресурсов")
    @ApiResponse(responseCode = "200", description = "Поиск ресурсов успешно выполнен")
    @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий поисковый запрос")
    @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    List<PathResponse> searchResources(@RequestParam String query, @AuthenticationPrincipal AuthenticatedUser user);
}
