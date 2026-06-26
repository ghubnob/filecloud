package dev.vivim.filecloud.other;

import dev.vivim.filecloud.dto.UserStorageRoot;
import dev.vivim.filecloud.exception.InvalidPathException;
import dev.vivim.filecloud.infrastructure.paths.PathObject;
import dev.vivim.filecloud.infrastructure.paths.PathResolver;
import dev.vivim.filecloud.infrastructure.paths.s3keys.S3PathResolver;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class S3PathsTests {
    PathResolver pathResolver = new S3PathResolver();
    UserStorageRoot root = new UserStorageRoot("user-1-files");

    @Test
    void shouldNullOrBlankPathResolveToRoot() {
        PathObject nullPath = pathResolver.resolve(null, root);
        assertTrue(nullPath.isDirectory());
        assertEquals("", nullPath.getBareName());
        assertEquals("", nullPath.getPrefix());
        assertEquals("user-1-files/", nullPath.getFullPath());
        PathObject blankPath = pathResolver.resolve("", root);
        assertEquals("", blankPath.getBareName());
        assertEquals("", blankPath.getPrefix());
        assertEquals("user-1-files/", blankPath.getFullPath());
    }

    @Test
    void shouldOnlySlashResolveToRoot() {
        PathObject path = pathResolver.resolve("/", root);
        assertTrue(path.isDirectory());
        assertEquals("", path.getBareName());
        assertEquals("", path.getPrefix());
    }

    @Test
    void shouldSimpleFileResolveCorrectly() {
        PathObject path = pathResolver.resolve("file.txt", root);
        assertFalse(path.isDirectory());
        assertEquals("file.txt", path.getBareName());
        assertEquals("file.txt", path.getLastSegment());
        assertEquals("", path.getPrefix());
        assertEquals("user-1-files/file.txt", path.getFullPath());
    }

    @Test
    void shouldSimpleFolderResolveCorrectly() {
        PathObject path = pathResolver.resolve("folder/", root);
        assertTrue(path.isDirectory());
        assertEquals("folder", path.getBareName());
        assertEquals("folder/", path.getLastSegment());
        assertEquals("", path.getPrefix());
        assertEquals("user-1-files/folder/", path.getFullPath());
    }

    @Test
    void shouldNestedFilePathResolveCorrectly() {
        PathObject path = pathResolver.resolve("folder1/folder2/file.txt", root);
        assertFalse(path.isDirectory());
        assertEquals("folder1/folder2/", path.getPrefix());
        assertEquals("file.txt", path.getBareName());
        assertEquals("file.txt", path.getLastSegment());
        assertEquals("user-1-files/folder1/folder2/file.txt", path.getFullPath());
    }

    @Test
    void shouldNestedFolderPathResolveCorrectly() {
        PathObject path = pathResolver.resolve("folder1/folder2/folder3/", root);
        assertTrue(path.isDirectory());
        assertEquals("folder1/folder2/", path.getPrefix());
        assertEquals("folder3", path.getBareName());
        assertEquals("folder3/", path.getLastSegment());
        assertEquals("user-1-files/folder1/folder2/folder3/", path.getFullPath());
    }

    @Test
    void shouldCollapseDoubleSlash() {
        PathObject path = pathResolver.resolve("folder1//file.txt", root);
        assertFalse(path.isDirectory());
        assertEquals("file.txt", path.getLastSegment());
        assertEquals("folder1/", path.getPrefix());
        assertEquals("user-1-files/folder1/file.txt", path.getFullPath());
    }

    @Test
    void shouldStripLeadingSlash() {
        PathObject path = pathResolver.resolve("/folder/file.txt", root);
        assertEquals("user-1-files/folder/file.txt", path.getFullPath());
        assertEquals("folder/", path.getPrefix());
        assertEquals("file.txt", path.getLastSegment());
    }

    @Test
    void shouldNormalizeDotSegment() {
        assertEquals("folder/", pathResolver.resolve("./folder/", root).getLastSegment());
    }

    @Test
    void shouldResolveDotDotToNormalPath() {
        PathObject path = pathResolver.resolve("folder/../file.txt", root);
        assertEquals("", path.getPrefix()); // folder и .. гасят друг друга - сначала идем в folder, затем выходим через ..
        assertEquals("file.txt", path.getBareName());
    }

    @Test
    void shouldThrowPureDot() {
        assertThrows(InvalidPathException.class, () -> pathResolver.resolve("..", root));
    }

    @Test
    void shouldRejectTripleDotSegment() {
        assertThrows(InvalidPathException.class, () -> pathResolver.resolve(".../file.txt", root));
    }

    @Test
    void shouldRejectExitUserRootDirectoryAttempt() {
        assertThrows(InvalidPathException.class, () -> pathResolver.resolve("../../user-2-files/", root));
    }
}
