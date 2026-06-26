package dev.vivim.filecloud.dto.response;

import dev.vivim.filecloud.dto.FileType;
import dev.vivim.filecloud.model.ResourceEntity;

public record PathResponse(String path, String name, Long size, FileType type) {
    public static PathResponse from(ResourceEntity r) {
        boolean isDir = r.getResourceType() == FileType.DIRECTORY;
        return new PathResponse(
                r.getPath(),
                isDir ? r.getName()+"/" : r.getName(),
                isDir ? null : r.getSize(),
                r.getResourceType());
    }
}