package dev.vivim.filecloud.infrastructure.paths.s3keys;

import dev.vivim.filecloud.dto.UserStorageRoot;
import dev.vivim.filecloud.exception.InvalidPathException;
import dev.vivim.filecloud.infrastructure.paths.PathObject;
import dev.vivim.filecloud.infrastructure.paths.PathResolver;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class S3PathResolver implements PathResolver {
    private static final Pattern DOT_ONLY = Pattern.compile("^\\.+$");
    private static final int MAX_SIZE_SEGMENT = 200;
    private static final Set<Character> INVALID_CHARS = Set.of(
            '/', '\\', ':', '*', '?', '"', '<', '>', '|'
    );

    @Override
    public PathObject resolve(String path, UserStorageRoot root) {
        log.debug("[PATH RESOLVER] Resolving path: {}", path);
        if (path==null || path.isBlank()) {
            log.debug("[PATH RESOLVER] Path resolved successfully (empty): {}/{}", root.value(), path);
            return new S3PathObject(root.value(), path);
        }
        boolean isFolder = path.endsWith("/");

        List<String> segments = splitAndValidate(normalize(path));
        if (segments.isEmpty()) {
            log.debug("[PATH RESOLVER] Path resolved successfully (empty): {}/{}", root.value(), path);
            return new S3PathObject(root.value(), "");
        }
        String relativePath = String.join("/", segments) + (isFolder ? "/" : "");

        log.debug("[PATH RESOLVER] Path resolved successfully: {}", relativePath);
        return new S3PathObject(root.value(), relativePath);
    }

    private String normalize(String rawPath) {
        log.debug("[PATH RESOLVER] Normalizing raw path: {}", rawPath);
        var res = Path.of(rawPath.replaceFirst("^/+", ""))
                .normalize()
                .toString()
                .replace("\\", "/");
        log.debug("[PATH RESOLVER] Successfully normalized path: {}", res);
        return res;
    }

    private List<String> splitAndValidate(String normalizedPath) {
        List<String> segments = Arrays.stream(normalizedPath.split("/"))
                .filter(p -> !p.isBlank())
                .toList();

        for (String segment : segments) {
            validateSegment(segment);
        }

        return segments;
    }

    private void validateSegment(String segment) {
        if (segment.length() > MAX_SIZE_SEGMENT) throw new InvalidPathException("Path segment is too long!");
        if (segment.isBlank()) throw new InvalidPathException("Path segment is empty!");
        if (DOT_ONLY.matcher(segment).matches()) throw new InvalidPathException("Path segment cannot contain only dots!");
        for (char c : segment.toCharArray()) {
            if (INVALID_CHARS.contains(c)) throw new InvalidPathException("Path segment contains invalid symbols!");
        }
    }
}
