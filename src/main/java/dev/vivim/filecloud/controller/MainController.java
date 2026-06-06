package dev.vivim.filecloud.controller;

import dev.vivim.filecloud.dto.*;
import dev.vivim.filecloud.service.FileService;
import dev.vivim.filecloud.service.UserService;
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
public class MainController {
    private final UserService userService;
    private final FileService fileService;

    public MainController(UserService userService, FileService fileService) {
        this.userService = userService;
        this.fileService = fileService;
    }

    @PostMapping("/auth/sign-up")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request, HttpServletRequest req, HttpServletResponse resp) {
        log.info("Registation Request: {}", request);

        Authentication authentication = new UsernamePasswordAuthenticationToken(request.username(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), req, resp);

        return userService.register(request);
    }

    @PostMapping("/auth/sign-in")
    public UserResponse authorization(@Valid @RequestBody AuthorizationRequest request, HttpServletRequest req, HttpServletResponse resp) {
        log.info("Authorization Request: {}", request);

        Authentication authentication = new UsernamePasswordAuthenticationToken(request.username(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), req, resp);

        return userService.authorization(request);
    }

    @GetMapping("/user/me")
    public UserResponse getMe(@AuthenticationPrincipal String username) {
        return new UserResponse(username);
    }

    @GetMapping("/resource")
    public PathResponse getResource(@RequestParam String path, @AuthenticationPrincipal String username) {
        log.info("Resource Request: {}/{}", username, path);
        return fileService.getResource(path, username);
    }

    @GetMapping("/resource/download")
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
    public PathResponse moveResource(@RequestParam String from, @RequestParam String to, @AuthenticationPrincipal String username) {
        log.info("Moving resource Request: from {}/{} to {}/{}", username, from, username, to);
        return fileService.moveResource(from, to, username);
    }

    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@RequestParam String path, @AuthenticationPrincipal String username) {
        log.info("Delete resource Request: {}/{}", username, path);
        fileService.deleteResource(path, username);
    }

    @PostMapping("/resource")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PathResponse> uploadResource(@RequestParam String path,
                                             @AuthenticationPrincipal String username,
                                             @RequestPart("object") List<MultipartFile> multipartFile) throws IOException {
        log.info("Upload resource Request: {}/{}, uploading {} files", username, path, multipartFile.size());
        return fileService.uploadResource(path, username, multipartFile);
    }

    @GetMapping("/directory")
    public List<PathResponse> getDirectory(@RequestParam String path, @AuthenticationPrincipal String username) {
        log.info("Directory Request: {}/{}", username, path);
        return fileService.getDirectory(path, username);
    }

    @GetMapping("/resource/search")
    public List<PathResponse> searchResources(@RequestParam String query, @AuthenticationPrincipal String username) {
        log.info("Search Request: {}. User: {}", query, username);
        return fileService.searchResources(query, username);
    }
}
