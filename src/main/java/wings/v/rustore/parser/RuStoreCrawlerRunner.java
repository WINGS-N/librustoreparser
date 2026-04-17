package wings.v.rustore.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RuStoreCrawlerRunner {
    private final RuStoreCrawlerPersistence persistence;

    public RuStoreCrawlerRunner() {
        this(new RuStoreCrawlerPersistence());
    }

    RuStoreCrawlerRunner(RuStoreCrawlerPersistence persistence) {
        this.persistence = Objects.requireNonNull(persistence, "persistence");
    }

    public CrawlExecutionResult runIncremental(
            RuStoreCrawlerConfig config,
            Path outputJsonPath,
            Path stateDirectory
    ) throws IOException, InterruptedException {
        return execute(config, outputJsonPath, stateDirectory, Mode.INCREMENTAL);
    }

    public CrawlExecutionResult rerunFails(
            RuStoreCrawlerConfig config,
            Path outputJsonPath,
            Path stateDirectory
    ) throws IOException, InterruptedException {
        return execute(config, outputJsonPath, stateDirectory, Mode.RERUN_FAILS);
    }

    public CrawlExecutionResult overwriteAll(
            RuStoreCrawlerConfig config,
            Path outputJsonPath,
            Path stateDirectory
    ) throws IOException, InterruptedException {
        return execute(config, outputJsonPath, stateDirectory, Mode.OVERWRITE_ALL);
    }

    private CrawlExecutionResult execute(
            RuStoreCrawlerConfig config,
            Path outputJsonPath,
            Path stateDirectory,
            Mode mode
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(outputJsonPath, "outputJsonPath");
        Objects.requireNonNull(stateDirectory, "stateDirectory");

        Path failsJsonPath = stateDirectory.resolve(RuStoreCrawlerPersistence.FAILS_FILE_NAME);
        List<AppInfo> existingApps = mode == Mode.OVERWRITE_ALL
                ? List.of()
                : persistence.loadApps(outputJsonPath).apps();
        List<CrawlFailure> existingFailures = mode == Mode.OVERWRITE_ALL
                ? List.of()
                : persistence.loadFailures(failsJsonPath);

        RunSelection selection = selectPendingSources(config, existingApps, existingFailures, mode);
        List<AppInfo> mergedApps = new ArrayList<>(existingApps);
        List<CrawlFailure> failures;

        if (selection.seedDeveloperIds().isEmpty() && selection.directPackages().isEmpty()) {
            failures = mode == Mode.RERUN_FAILS ? List.of() : existingFailures;
        } else {
            RuStoreCrawler crawler = new RuStoreCrawler(config.toBuilder()
                    .seedDeveloperIds(selection.seedDeveloperIds())
                    .directPackages(selection.directPackages())
                    .build());
            CrawlResult crawlResult = crawler.crawl();
            mergedApps = mergeApps(existingApps, crawlResult.getApps(), mode == Mode.OVERWRITE_ALL);
            failures = crawlResult.getFailures();
        }

        if (mergedApps.isEmpty()) {
            throw new IOException("no apps available to write to output json");
        }

        persistence.writeApps(outputJsonPath, mergedApps);
        persistence.writeFailures(failsJsonPath, failures);
        return new CrawlExecutionResult(mergedApps, failures, outputJsonPath, failsJsonPath);
    }

    private RunSelection selectPendingSources(
            RuStoreCrawlerConfig config,
            List<AppInfo> existingApps,
            List<CrawlFailure> existingFailures,
            Mode mode
    ) {
        if (mode == Mode.RERUN_FAILS) {
            return selectFailuresOnly(config, existingFailures);
        }
        if (mode == Mode.OVERWRITE_ALL) {
            return new RunSelection(config.getSeedDeveloperIds(), config.getDirectPackages());
        }

        Set<String> completedDeveloperIds = new LinkedHashSet<>();
        Set<String> completedPackages = new LinkedHashSet<>();
        for (AppInfo app : existingApps) {
            completedPackages.add(app.getPackageName());
            String developerPath = app.getDeveloperPath();
            if (developerPath != null && developerPath.startsWith("/catalog/developer/")) {
                completedDeveloperIds.add(developerPath.substring("/catalog/developer/".length()));
            }
        }

        List<String> seedDeveloperIds = new ArrayList<>();
        for (String developerId : config.getSeedDeveloperIds()) {
            if (!completedDeveloperIds.contains(developerId)) {
                seedDeveloperIds.add(developerId);
            }
        }

        List<String> directPackages = new ArrayList<>();
        for (String packageName : config.getDirectPackages()) {
            if (!completedPackages.contains(packageName)) {
                directPackages.add(packageName);
            }
        }

        return new RunSelection(seedDeveloperIds, directPackages);
    }

    private RunSelection selectFailuresOnly(RuStoreCrawlerConfig config, List<CrawlFailure> existingFailures) {
        Set<String> allowedDeveloperIds = new LinkedHashSet<>(config.getSeedDeveloperIds());
        Set<String> allowedDirectPackages = new LinkedHashSet<>(config.getDirectPackages());
        List<String> seedDeveloperIds = new ArrayList<>();
        List<String> directPackages = new ArrayList<>();

        for (CrawlFailure failure : existingFailures) {
            if (failure.getSourceType() == CrawlFailure.SourceType.DEVELOPER) {
                String developerId = failure.getSourceId();
                if (developerId != null && allowedDeveloperIds.contains(developerId) && !seedDeveloperIds.contains(developerId)) {
                    seedDeveloperIds.add(developerId);
                }
            } else if (failure.getSourceType() == CrawlFailure.SourceType.DIRECT_PACKAGE) {
                String packageName = failure.getSourceId();
                if (packageName != null && allowedDirectPackages.contains(packageName) && !directPackages.contains(packageName)) {
                    directPackages.add(packageName);
                }
            }
        }

        return new RunSelection(seedDeveloperIds, directPackages);
    }

    private List<AppInfo> mergeApps(List<AppInfo> existingApps, List<AppInfo> newApps, boolean overwriteAll) {
        Map<String, AppInfo> byPackage = new LinkedHashMap<>();
        if (!overwriteAll) {
            for (AppInfo app : existingApps) {
                byPackage.put(app.getPackageName(), app);
            }
        }
        for (AppInfo app : newApps) {
            AppInfo current = byPackage.get(app.getPackageName());
            if (current == null || shouldReplace(current, app)) {
                byPackage.put(app.getPackageName(), app);
            }
        }
        return new ArrayList<>(byPackage.values());
    }

    private boolean shouldReplace(AppInfo current, AppInfo candidate) {
        if (current.getAppName() == null && candidate.getAppName() != null) {
            return true;
        }
        if (current.getDeveloperName() == null && candidate.getDeveloperName() != null) {
            return true;
        }
        if (current.getDeveloperPath() == null && candidate.getDeveloperPath() != null) {
            return true;
        }
        return false;
    }

    private enum Mode {
        INCREMENTAL,
        RERUN_FAILS,
        OVERWRITE_ALL
    }

    private record RunSelection(List<String> seedDeveloperIds, List<String> directPackages) {
    }
}
