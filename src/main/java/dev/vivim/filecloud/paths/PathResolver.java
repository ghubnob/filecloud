package dev.vivim.filecloud.paths;

public interface PathResolver {
    PathObject resolve(String path, String parent);
}
