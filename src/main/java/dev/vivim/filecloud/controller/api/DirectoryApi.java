package dev.vivim.filecloud.controller.api;

import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.dto.response.PathResponse;
import dev.vivim.filecloud.dto.request.PathRequest;
import dev.vivim.filecloud.dto.annotation.ResourceOperationResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/directory")
@Tag(name = "Directories", description = "Управление папками файлового хранилища")
public interface DirectoryApi {
    @GetMapping
    @Operation(summary = "Получение информации о содержимом папки")
    @ApiResponse(responseCode = "200", description = "Информация о папке успешно получена")
    @ResourceOperationResponses
    List<PathResponse> getDirectory(@RequestParam(required = false, defaultValue = "") String path,
                                    @AuthenticationPrincipal AuthenticatedUser user);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создание пустой папки")
    @ApiResponse(responseCode = "201", description = "Пустая папка создана")
    @ResourceOperationResponses
    @ApiResponse(responseCode = "409", description = "Папка уже существует")
    PathResponse createDirectory(@Valid PathRequest pathReq, @AuthenticationPrincipal AuthenticatedUser user);
}
