package dev.vivim.filecloud.paths.minio;

import dev.vivim.filecloud.paths.PathObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MinIOPathObject implements PathObject {
    private final String rootFolder;
    private final String path;

    public MinIOPathObject(String rootFolder, String path) {
        this.rootFolder = rootFolder==null ? "" : rootFolder;
        this.path = path==null ? "" : path;
    }

    @Override
    public String getFullPath() {
        return rootFolder + "/" + path;
    }

    @Override
    public String getLastSegmentName() {
        String fullPath = path;
        if (fullPath.isBlank() || "/".equals(fullPath)) return "";

        String cleanPath = isDirectory() ? fullPath.substring(0, fullPath.length()-1) : fullPath;
        log.debug("[PATH RESOLVER] Getting last segment name. Clean path: {}", cleanPath);

        int lastSlash = cleanPath.lastIndexOf('/');
        String result = lastSlash<0 ? cleanPath : cleanPath.substring(lastSlash+1);
        log.debug("[PATH RESOLVER] Getting last segment name. Result: {}", result);
        return result;
    }

    @Override
    public String getLastSegment() {
        return isDirectory() ? getLastSegmentName()+"/" : getLastSegmentName();
    }

    @Override
    public String getPrefix() {
        String fullPath = this.path;
        if (fullPath.isBlank() || "/".equals(fullPath)) return "";

        String cleanPath = isDirectory() ? fullPath.substring(0, fullPath.length()-1) : fullPath;
        log.debug("[PATH OBJECT] Getting prefix. Clean path: {}", cleanPath);

        int lastSlash = cleanPath.lastIndexOf("/");
        String result = lastSlash<0 ? "" : cleanPath.substring(0, lastSlash+1);
        log.debug("[PATH OBJECT] Getting prefix. Result: {}", result);
        return result;
    }

    @Override
    public boolean isDirectory() {
        return path.isBlank() || path.endsWith("/");
    }
}
