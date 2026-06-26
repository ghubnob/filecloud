package dev.vivim.filecloud.service;

import dev.vivim.filecloud.dto.DownloadContainer;
import dev.vivim.filecloud.dto.response.PathResponse;
import dev.vivim.filecloud.events.UserRegisteredEvent;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {
    PathResponse getResource(String path, Integer parentPrefix);
    List<PathResponse> getDirectory(String path, Integer parentPrefix);
    List<PathResponse> searchResources(String query, Integer parentPrefix);
    List<PathResponse> uploadResource(String path, Integer parentPrefix, List<MultipartFile> files) throws IOException;
    PathResponse moveResource(String from, String to, Integer parentPrefix);
    void deleteResource(String path, Integer parentPrefix);
    DownloadContainer downloadResource(String path, Integer parentPrefix);
    PathResponse createDirectory(String path, Integer parentPrefix);
    void onUserRegistered(UserRegisteredEvent event);
}
