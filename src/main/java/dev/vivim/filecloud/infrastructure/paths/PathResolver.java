package dev.vivim.filecloud.infrastructure.paths;

import dev.vivim.filecloud.dto.UserStorageRoot;

public interface PathResolver {
    PathObject resolve(String path, UserStorageRoot root);
}
