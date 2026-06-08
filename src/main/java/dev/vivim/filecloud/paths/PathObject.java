package dev.vivim.filecloud.paths;

public interface PathObject {
    String getFullPath();
    String getLastSegment();
    String getPrefix();
    boolean isDirectory();
}
