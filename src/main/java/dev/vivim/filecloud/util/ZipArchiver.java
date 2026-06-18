package dev.vivim.filecloud.util;

import dev.vivim.filecloud.dto.storage.StorageFileSummary;
import dev.vivim.filecloud.exception.DownloadException;
import dev.vivim.filecloud.infrastructure.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
public class ZipArchiver {
    private final ObjectStorage objectStorage;

    public void archive(String s3Key, OutputStream out) {
        List<StorageFileSummary> files = objectStorage.getAllObjectsByPrefix(s3Key);
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (var file : files) {
                String relativePath = file.key().substring(s3Key.length());
                if (relativePath.isEmpty()) continue;

                zos.putNextEntry(new ZipEntry(relativePath));
                if (!file.key().endsWith("/")) {
                    try (var obj = objectStorage.getObjectContent(file.key())) {
                        obj.transferTo(zos);
                    }
                }

                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new DownloadException("Error downloading files!");
        }
    }
}
