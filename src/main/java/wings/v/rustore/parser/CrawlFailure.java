package wings.v.rustore.parser;

import java.util.Objects;

public final class CrawlFailure {
    public enum SourceType {
        DEVELOPER,
        DIRECT_PACKAGE
    }

    private final SourceType sourceType;
    private final String sourceId;
    private final String sourcePath;
    private final String sourceName;
    private final String message;

    public CrawlFailure(
            SourceType sourceType,
            String sourceId,
            String sourcePath,
            String sourceName,
            String message
    ) {
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.sourceId = emptyToNull(sourceId);
        this.sourcePath = emptyToNull(sourcePath);
        this.sourceName = emptyToNull(sourceName);
        this.message = Objects.requireNonNullElse(message, "unknown error");
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getMessage() {
        return message;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
