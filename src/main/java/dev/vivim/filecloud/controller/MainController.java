package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.dto.*;
import dev.vivim.filecloud.service.FileService;
import dev.vivim.filecloud.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequestMapping("/api")
@RestController
@Slf4j
@Tag(name = "Файловое хранилище", description = "Методы для работы с файловым хранилищем и регистрацией/авторизацией")
public class MainController {
    private final UserService userService;
    private final FileService fileService;

    public MainController(UserService userService, FileService fileService) {
        this.userService = userService;
        this.fileService = fileService;
    }


    @PostMapping("/auth/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация пользователя")
    @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации (например слишком короткий username)")
    @ApiResponse(responseCode = "409", description = "Ошибка - пользователь с таким именем уже существует")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request, HttpServletRequest req, HttpServletResponse resp) {
        log.info("Registation Request: {}", request);

        Authentication authentication = new UsernamePasswordAuthenticationToken(request.username(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), req, resp);

        return userService.register(request);
    }


    @PostMapping("/auth/sign-in")
    @Operation(summary = "Авторизация пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь успешно авторизован")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации (например слишком короткий username)")
    @ApiResponse(responseCode = "401", description = "Ошибка - введены неверные данные")
    public UserResponse authorization(@Valid @RequestBody AuthorizationRequest request, HttpServletRequest req, HttpServletResponse resp) {
        log.info("Authorization Request: {}", request);

        Authentication authentication = new UsernamePasswordAuthenticationToken(request.username(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), req, resp);

        return userService.authorization(request);
    }


    @GetMapping("/user/me")
    @Operation(summary = "Получение usename пользователя")
    @ApiResponse(responseCode = "200", description = "Username пользователя получен")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    public UserResponse getMe(@AuthenticationPrincipal String username) {
        log.info("Get authorized user's name request: {}", username);
        return new UserResponse(username);
    }


    @GetMapping("/resource")
    @Operation(summary = "Получение информации о ресурсе")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно получен")
    @ApiResponse(responseCode = "400", description = "Ошибка - невалидный или отсутствующий путь")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    @ApiResponse(responseCode = "404", description = "Ошибка - ресурс не найден")
    public PathResponse getResource(@RequestParam String path, @AuthenticationPrincipal String username) {
        log.info("Resource Request: {}/{}", username, path);
        return fileService.getResource(path, username);
    }


    @GetMapping("/resource/download")
    @Operation(summary = "Скачивание ресурса")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно начал скачиваться")
    @ApiResponse(responseCode = "400", description = "Ошибка - невалидный или отсутствующий путь")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    @ApiResponse(responseCode = "404", description = "Ошибка - ресурс не найден")
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam String path,
                                                   @AuthenticationPrincipal String username) {
        log.info("Download resource Request: {}/{}", username, path);
        DownloadContainer container = fileService.downloadResource(path, username);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(container.contentType());
        headers.setContentDisposition(ContentDisposition.attachment().filename(container.fileName(), StandardCharsets.UTF_8).build());

        StreamingResponseBody body = outputStream -> container.writer().accept(outputStream);
        return ResponseEntity.ok().headers(headers).body(body);
    }


    @GetMapping("/resource/move")
    @Operation(summary = "Переименование/перемещение ресурса")
    @ApiResponse(responseCode = "200", description = "Ресурс успешно перемещен")
    @ApiResponse(responseCode = "400", description = "Ошибка - невалидный или отсутствующий путь")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    @ApiResponse(responseCode = "404", description = "Ошибка - ресурс не найден")
    @ApiResponse(responseCode = "409", description = "Ошибка - ресурс на пути 'to' уже существует")
    public PathResponse moveResource(@RequestParam String from, @RequestParam String to, @AuthenticationPrincipal String username) {
        log.info("Moving resource Request: from {}/{} to {}/{}", username, from, username, to);
        return fileService.moveResource(from, to, username);
    }


    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление ресурса")
    @ApiResponse(responseCode = "204", description = "Ресурс успешно удален")
    @ApiResponse(responseCode = "400", description = "Ошибка - невалидный или отсутствующий путь")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    @ApiResponse(responseCode = "404", description = "Ошибка - ресурс не найден")
    public void deleteResource(@RequestParam String path, @AuthenticationPrincipal String username) {
        log.info("Delete resource Request: {}/{}", username, path);
        fileService.deleteResource(path, username);
    }


    @PostMapping("/resource")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Загрузка ресурса на хранилище")
    @ApiResponse(responseCode = "201", description = "Ресурс успешно начал загружаться")
    @ApiResponse(responseCode = "400", description = "Ошибка - невалидное тело запроса")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    @ApiResponse(responseCode = "409", description = "Ошибка - файл уже существует")
    public List<PathResponse> uploadResource(@RequestParam String path,
                                             @AuthenticationPrincipal String username,
                                             @RequestPart("object") List<MultipartFile> multipartFile) throws IOException {
        log.info("Upload resource Request: {}/{}, uploading {} files", username, path, multipartFile.size());
        return fileService.uploadResource(path, username, multipartFile);
    }


    @GetMapping("/directory")
    @Operation(summary = "Получение информации о содержимом папки")
    @ApiResponse(responseCode = "200", description = "Информация о папке успешно получена")
    @ApiResponse(responseCode = "400", description = "Ошибка - невалидный или отсутствующий путь")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    @ApiResponse(responseCode = "404", description = "Ошибка - папка не найдена")
    public List<PathResponse> getDirectory(@RequestParam String path, @AuthenticationPrincipal String username) {
        log.info("Directory Request: {}/{}", username, path);
        return fileService.getDirectory(path, username);
    }


    @GetMapping("/resource/search")
    @Operation(summary = "Поиск ресурсов")
    @ApiResponse(responseCode = "200", description = "Поиск ресурсов успешно выполнен")
    @ApiResponse(responseCode = "400", description = "Ошибка - невалидный или отсутствующий поисковый запрос")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    public List<PathResponse> searchResources(@RequestParam String query, @AuthenticationPrincipal String username) {
        log.info("Search Request: {}. User: {}", query, username);
        return fileService.searchResources(query, username);
    }


    @PostMapping("/directory")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создание пустой папки")
    @ApiResponse(responseCode = "201", description = "Пустая папка создана")
    @ApiResponse(responseCode = "400", description = "Ошибка - невалидный или отсутствующий путь")
    @ApiResponse(responseCode = "401", description = "Ошибка - пользователь не авторизован")
    @ApiResponse(responseCode = "404", description = "Ошибка - родительская папка не найдена")
    @ApiResponse(responseCode = "409", description = "Ошибка - папка уже существует")
    public PathResponse createDirectory(@RequestParam String path, @AuthenticationPrincipal String username) {
        log.info("Create empty directory Request: {}/{}", username, path);
        return fileService.createDirectory(path, username);
    }
}
