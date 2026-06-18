package dev.vivim.filecloud.infrastructure.paths;

public interface PathObject {
    String getFullPath();
    String getLastSegment();
    String getPrefix();
    boolean isDirectory();
}
