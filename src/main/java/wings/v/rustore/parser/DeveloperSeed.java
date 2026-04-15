package wings.v.rustore.parser;

import java.util.Objects;

public final class DeveloperSeed {
    private final String id;
    private final String path;
    private final String name;

    public DeveloperSeed(String id, String path, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.path = Objects.requireNonNull(path, "path");
        this.name = emptyToNull(name);
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
