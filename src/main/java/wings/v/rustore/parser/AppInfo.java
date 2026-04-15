package wings.v.rustore.parser;

import java.util.Objects;

public final class AppInfo {
    private final String packageName;
    private final String appName;
    private final String developerName;
    private final String developerPath;

    public AppInfo(String packageName, String appName, String developerName, String developerPath) {
        this.packageName = Objects.requireNonNull(packageName, "packageName");
        this.appName = emptyToNull(appName);
        this.developerName = emptyToNull(developerName);
        this.developerPath = emptyToNull(developerPath);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public String getDeveloperPath() {
        return developerPath;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
