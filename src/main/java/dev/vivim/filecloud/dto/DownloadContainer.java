package dev.vivim.filecloud.dto;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record DownloadContainer(StreamingResponseBody fileStream,
                                String fileName,
                                MediaType mediaType) {
    public static DownloadContainer folder(StreamingResponseBody fileStream, String fileName) {
        return new DownloadContainer(fileStream, fileName+".zip", MediaType.parseMediaType("application/zip"));
    }
}
