package dev.vivim.filecloud.dto;

import org.springframework.http.MediaType;

import java.io.OutputStream;
import java.util.function.Consumer;

public record DownloadContainer(Consumer<OutputStream> writer,
                                String fileName,
                                MediaType contentType) {
    public static DownloadContainer file(Consumer<OutputStream> writer, String fileName, MediaType contentType) {
        return new DownloadContainer(writer, fileName, contentType);
    }
    public static DownloadContainer folder(Consumer<OutputStream> writer, String fileName) {
        return new DownloadContainer(writer, fileName, MediaType.parseMediaType("application/zip"));
    }
}
