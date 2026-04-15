package wings.v.rustore.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CrawlResult {
    private final List<AppInfo> apps;
    private final List<DeveloperSeed> developers;
    private final List<String> packageNames;

    public CrawlResult(List<AppInfo> apps, List<DeveloperSeed> developers) {
        Objects.requireNonNull(apps, "apps");
        Objects.requireNonNull(developers, "developers");
        this.apps = Collections.unmodifiableList(new ArrayList<>(apps));
        this.developers = Collections.unmodifiableList(new ArrayList<>(developers));

        List<String> packages = new ArrayList<>(apps.size());
        for (AppInfo app : apps) {
            packages.add(app.getPackageName());
        }
        this.packageNames = Collections.unmodifiableList(packages);
    }

    public List<AppInfo> getApps() {
        return apps;
    }

    public List<DeveloperSeed> getDevelopers() {
        return developers;
    }

    public List<String> getPackageNames() {
        return packageNames;
    }
}
