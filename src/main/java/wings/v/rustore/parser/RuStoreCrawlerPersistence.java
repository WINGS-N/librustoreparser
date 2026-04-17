package wings.v.rustore.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RuStoreCrawlerPersistence {
    static final String FAILS_FILE_NAME = "librustoreparser_fails.json";

    PersistedData loadApps(Path outputJsonPath) throws IOException {
        if (outputJsonPath == null || !Files.isRegularFile(outputJsonPath)) {
            return new PersistedData(List.of(), List.of());
        }
        Object parsed = SimpleJson.parse(Files.readString(outputJsonPath, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> root)) {
            return new PersistedData(List.of(), List.of());
        }
        Object packagesRaw = root.get("packages");
        if (!(packagesRaw instanceof List<?> packages)) {
            return new PersistedData(List.of(), List.of());
        }
        List<AppInfo> apps = new ArrayList<>();
        for (Object item : packages) {
            if (!(item instanceof Map<?, ?> object)) {
                continue;
            }
            String packageName = stringValue(object.get("package_name"));
            if (packageName == null || packageName.isBlank()) {
                continue;
            }
            apps.add(new AppInfo(
                    packageName,
                    stringValue(object.get("app_name")),
                    stringValue(object.get("developer_name")),
                    stringValue(object.get("developer_path"))
            ));
        }
        return new PersistedData(apps, List.of());
    }

    List<CrawlFailure> loadFailures(Path failsJsonPath) throws IOException {
        if (failsJsonPath == null || !Files.isRegularFile(failsJsonPath)) {
            return List.of();
        }
        Object parsed = SimpleJson.parse(Files.readString(failsJsonPath, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> root)) {
            return List.of();
        }
        Object failuresRaw = root.get("failures");
        if (!(failuresRaw instanceof List<?> failureItems)) {
            return List.of();
        }
        List<CrawlFailure> failures = new ArrayList<>();
        for (Object item : failureItems) {
            if (!(item instanceof Map<?, ?> object)) {
                continue;
            }
            String typeValue = stringValue(object.get("source_type"));
            CrawlFailure.SourceType type;
            try {
                type = CrawlFailure.SourceType.valueOf(typeValue == null ? "" : typeValue);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            failures.add(new CrawlFailure(
                    type,
                    stringValue(object.get("source_id")),
                    stringValue(object.get("source_path")),
                    stringValue(object.get("source_name")),
                    stringValue(object.get("message"))
            ));
        }
        return failures;
    }

    void writeApps(Path outputJsonPath, List<AppInfo> apps) throws IOException {
        ensureParentDirectory(outputJsonPath);
        List<AppInfo> sortedApps = new ArrayList<>(apps);
        sortedApps.sort(Comparator.comparing(AppInfo::getPackageName));

        List<Map<String, Object>> packageItems = new ArrayList<>(sortedApps.size());
        for (AppInfo app : sortedApps) {
            LinkedHashMap<String, Object> object = new LinkedHashMap<>();
            object.put("package_name", app.getPackageName());
            object.put("app_name", app.getAppName());
            object.put("developer_name", app.getDeveloperName());
            object.put("developer_path", app.getDeveloperPath());
            packageItems.add(object);
        }

        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("source", "librustoreparser");
        root.put("generated_at_utc", Instant.now().toString());
        root.put("package_count", sortedApps.size());
        root.put("packages", packageItems);

        Files.writeString(outputJsonPath, pretty(SimpleJson.stringify(root)), StandardCharsets.UTF_8);
    }

    void writeFailures(Path failsJsonPath, List<CrawlFailure> failures) throws IOException {
        ensureParentDirectory(failsJsonPath);
        List<Map<String, Object>> failureItems = new ArrayList<>(failures.size());
        for (CrawlFailure failure : failures) {
            LinkedHashMap<String, Object> object = new LinkedHashMap<>();
            object.put("source_type", failure.getSourceType().name());
            object.put("source_id", failure.getSourceId());
            object.put("source_path", failure.getSourcePath());
            object.put("source_name", failure.getSourceName());
            object.put("message", failure.getMessage());
            failureItems.add(object);
        }

        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("source", "librustoreparser");
        root.put("generated_at_utc", Instant.now().toString());
        root.put("failure_count", failures.size());
        root.put("failures", failureItems);

        Files.writeString(failsJsonPath, pretty(SimpleJson.stringify(root)), StandardCharsets.UTF_8);
    }

    private static void ensureParentDirectory(Path path) throws IOException {
        Path parent = path == null ? null : path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isEmpty() || "null".equals(string) ? null : string;
    }

    private static String pretty(String rawJson) {
        StringBuilder result = new StringBuilder(rawJson.length() + 64);
        int indent = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < rawJson.length(); i++) {
            char current = rawJson.charAt(i);
            if (inString) {
                result.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            switch (current) {
                case '{', '[' -> {
                    result.append(current).append('\n');
                    indent++;
                    appendIndent(result, indent);
                }
                case '}', ']' -> {
                    result.append('\n');
                    indent--;
                    appendIndent(result, indent);
                    result.append(current);
                }
                case ',' -> {
                    result.append(current).append('\n');
                    appendIndent(result, indent);
                }
                case ':' -> result.append(": ");
                case '"' -> {
                    inString = true;
                    result.append(current);
                }
                default -> result.append(current);
            }
        }
        return result.append('\n').toString();
    }

    private static void appendIndent(StringBuilder builder, int indent) {
        for (int i = 0; i < indent; i++) {
            builder.append("  ");
        }
    }

    record PersistedData(List<AppInfo> apps, List<CrawlFailure> failures) {
    }
}
