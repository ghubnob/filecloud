package dev.vivim.filecloud.paths;

public interface PathObject {
    String getFullPath();
    String getLastSegmentName();
    String getLastSegment();
    String getPrefix();
    boolean isDirectory();
}
