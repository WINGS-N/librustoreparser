package wings.v.rustore.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CrawlResult {
    private final List<AppInfo> apps;
    private final List<DeveloperSeed> developers;
    private final List<CrawlFailure> failures;
    private final List<String> packageNames;

    public CrawlResult(List<AppInfo> apps, List<DeveloperSeed> developers, List<CrawlFailure> failures) {
        Objects.requireNonNull(apps, "apps");
        Objects.requireNonNull(developers, "developers");
        Objects.requireNonNull(failures, "failures");
        this.apps = Collections.unmodifiableList(new ArrayList<>(apps));
        this.developers = Collections.unmodifiableList(new ArrayList<>(developers));
        this.failures = Collections.unmodifiableList(new ArrayList<>(failures));

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

    public List<CrawlFailure> getFailures() {
        return failures;
    }

    public List<String> getPackageNames() {
        return packageNames;
    }
}
