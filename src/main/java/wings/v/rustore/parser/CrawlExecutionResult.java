package wings.v.rustore.parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CrawlExecutionResult {
    private final List<AppInfo> apps;
    private final List<CrawlFailure> failures;
    private final Path outputJsonPath;
    private final Path failsJsonPath;

    public CrawlExecutionResult(
            List<AppInfo> apps,
            List<CrawlFailure> failures,
            Path outputJsonPath,
            Path failsJsonPath
    ) {
        Objects.requireNonNull(apps, "apps");
        Objects.requireNonNull(failures, "failures");
        this.apps = Collections.unmodifiableList(new ArrayList<>(apps));
        this.failures = Collections.unmodifiableList(new ArrayList<>(failures));
        this.outputJsonPath = outputJsonPath;
        this.failsJsonPath = failsJsonPath;
    }

    public List<AppInfo> getApps() {
        return apps;
    }

    public List<CrawlFailure> getFailures() {
        return failures;
    }

    public Path getOutputJsonPath() {
        return outputJsonPath;
    }

    public Path getFailsJsonPath() {
        return failsJsonPath;
    }
}
