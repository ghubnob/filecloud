package dev.vivim.filecloud.dto.storage;

import java.util.List;

public record StorageDirectoryContent(List<String> prefixes, List<StorageFileSummary> files) {}
