package wings.v.rustore.parser;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RuStoreCrawler {
    private final RuStoreCrawlerConfig config;
    private final RuStorePageFetcher pageFetcher;
    private final RequestThrottler throttler;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public RuStoreCrawler(RuStoreCrawlerConfig config) {
        this(config, new HttpPageFetcher(config.getRequestTimeout(), config.getUserAgent()));
    }

    public RuStoreCrawler(RuStoreCrawlerConfig config, RuStorePageFetcher pageFetcher) {
        this.config = Objects.requireNonNull(config, "config");
        this.pageFetcher = Objects.requireNonNull(pageFetcher, "pageFetcher");
        this.throttler = new RequestThrottler(config.getRequestDelay());
    }

    public CrawlResult crawl() throws IOException, InterruptedException {
        List<MutableDeveloperSeed> developers = collectSeedDevelopers();
        log("seed developers discovered: " + developers.size());

        if (config.getMaxSeedDevelopers() > 0 && developers.size() > config.getMaxSeedDevelopers()) {
            developers = new ArrayList<>(developers.subList(0, config.getMaxSeedDevelopers()));
        }
        log("seed developers selected for crawling: " + developers.size());
        progress("seed developers: " + developers.size());

        List<String> directPackages = collectDirectSeedPackages();
        log("direct packages configured: " + directPackages.size());
        progress("direct packages: " + directPackages.size());

        List<AppInfo> apps = collectDeveloperApps(developers);
        if (apps.isEmpty()) {
            throw new IOException("no apps collected from developer pages");
        }

        List<AppInfo> mergedApps = mergeDirectPackages(apps, directPackages);
        sortApps(mergedApps, developers);
        return new CrawlResult(mergedApps, freezeDevelopers(developers));
    }

    private List<MutableDeveloperSeed> collectSeedDevelopers() throws IOException {
        List<String> ids = config.getSeedDeveloperIds();
        if (ids.isEmpty()) {
            throw new IOException("no suspicious seed developers configured");
        }

        Map<String, MutableDeveloperSeed> seedsById = new LinkedHashMap<>();
        for (String rawId : ids) {
            String id = HtmlParsers.cleanText(rawId);
            if (id.isEmpty() || seedsById.containsKey(id)) {
                continue;
            }
            seedsById.put(id, new MutableDeveloperSeed(id, "/catalog/developer/" + id));
        }

        List<MutableDeveloperSeed> seeds = new ArrayList<>(seedsById.values());
        seeds.sort(Comparator.comparing(MutableDeveloperSeed::id));
        if (seeds.isEmpty()) {
            throw new IOException("no suspicious seed developers configured");
        }
        return seeds;
    }

    private List<String> collectDirectSeedPackages() {
        Map<String, String> packages = new LinkedHashMap<>();
        for (String rawPackageName : config.getDirectPackages()) {
            String packageName = HtmlParsers.cleanText(rawPackageName);
            if (!packageName.isEmpty()) {
                packages.put(packageName, packageName);
            }
        }
        return new ArrayList<>(packages.values());
    }

    private List<AppInfo> collectDeveloperApps(List<MutableDeveloperSeed> developers) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(config.getConcurrency());
        ExecutorCompletionService<DeveloperCrawlResult> completionService =
                new ExecutorCompletionService<>(executor);

        try {
            for (MutableDeveloperSeed seed : developers) {
                completionService.submit(() -> {
                    try {
                        return new DeveloperCrawlResult(seed, crawlDeveloper(seed.path()), null);
                    } catch (IOException exception) {
                        return new DeveloperCrawlResult(seed, null, exception);
                    }
                });
            }

            Map<String, AppInfo> appsByPackage = new HashMap<>();
            int completed = 0;
            int failed = 0;
            for (int i = 0; i < developers.size(); i++) {
                DeveloperCrawlResult result;
                String currentDeveloper = "unknown";
                try {
                    result = completionService.take().get();
                } catch (InterruptedException exception) {
                    executor.shutdownNow();
                    throw exception;
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof InterruptedException interruptedException) {
                        executor.shutdownNow();
                        throw interruptedException;
                    }
                    log("skip developer crawl: " + (cause == null ? exception.getMessage() : cause.getMessage()));
                    completed++;
                    failed++;
                    progress("developers " + completed + "/" + developers.size()
                            + " | apps " + appsByPackage.size()
                            + " | failed " + failed
                            + " | current " + currentDeveloper);
                    continue;
                }

                if (result.seed() != null) {
                    currentDeveloper = developerLabel(result.seed());
                }
                if (result.error() != null) {
                    log("skip developer crawl: " + result.error().getMessage());
                    completed++;
                    failed++;
                    progress("developers " + completed + "/" + developers.size()
                            + " | apps " + appsByPackage.size()
                            + " | failed " + failed
                            + " | current " + currentDeveloper);
                    continue;
                }

                DeveloperApps data = result.data();
                MutableDeveloperSeed seed = result.seed();
                if (seed != null && seed.name() == null && data.name() != null) {
                    seed.name(data.name());
                    currentDeveloper = developerLabel(seed);
                }
                if (seed != null) {
                    log("seed developer: " + seed.id() + " | " + coalesce(seed.name(), seed.path()));
                }

                for (AppInfo app : data.apps()) {
                    AppInfo current = appsByPackage.get(app.getPackageName());
                    if (current == null || betterAppInfo(app, current)) {
                        appsByPackage.put(app.getPackageName(), app);
                    }
                }
                completed++;
                progress("developers " + completed + "/" + developers.size()
                        + " | apps " + appsByPackage.size()
                        + " | failed " + failed
                        + " | current " + currentDeveloper);
            }

            return new ArrayList<>(appsByPackage.values());
        } finally {
            executor.shutdownNow();
        }
    }

    private List<AppInfo> mergeDirectPackages(List<AppInfo> apps, List<String> packages)
            throws IOException, InterruptedException {
        if (packages.isEmpty()) {
            return new ArrayList<>(apps);
        }

        Map<String, AppInfo> appsByPackage = new LinkedHashMap<>();
        for (AppInfo app : apps) {
            appsByPackage.put(app.getPackageName(), app);
        }

        for (String packageName : packages) {
            if (appsByPackage.containsKey(packageName)) {
                log("direct package already covered: " + packageName);
                continue;
            }

            String appName = packageName;
            String developerName = null;
            String developerPath = null;

            try {
                String body = fetch("/catalog/app/" + packageName);
                appName = coalesce(HtmlParsers.extractPageH1(body), packageName);
                developerName = HtmlParsers.extractAppCompanyName(body);
                developerPath = HtmlParsers.extractAppDeveloperPath(body);
            } catch (IOException exception) {
                log("direct package fetch failed: " + packageName + ": " + exception.getMessage());
            }

            AppInfo app = new AppInfo(packageName, appName, developerName, developerPath);
            appsByPackage.put(packageName, app);
            log("direct package: " + app.getPackageName() + " | " + coalesce(app.getAppName(), packageName)
                    + " | " + coalesce(app.getDeveloperName(), app.getDeveloperPath(), "direct-only"));
        }

        return new ArrayList<>(appsByPackage.values());
    }

    private DeveloperApps crawlDeveloper(String path) throws IOException, InterruptedException {
        String currentUrl = absolute(path);
        Map<String, Boolean> visited = new HashMap<>();
        Map<String, AppInfo> apps = new HashMap<>();
        String developerName = null;
        int pagesRead = 0;

        while (currentUrl != null && !currentUrl.isEmpty()) {
            if (visited.containsKey(currentUrl)) {
                break;
            }
            if (config.getMaxDeveloperPages() > 0 && pagesRead >= config.getMaxDeveloperPages()) {
                break;
            }
            visited.put(currentUrl, Boolean.TRUE);

            String body = fetch(currentUrl);
            if (developerName == null) {
                developerName = HtmlParsers.extractDeveloperName(body);
            }

            for (Map.Entry<String, String> entry : HtmlParsers.extractApps(body).entrySet()) {
                String packageName = entry.getKey();
                String discoveredName = entry.getValue();
                AppInfo previous = apps.get(packageName);

                AppInfo app = new AppInfo(
                        packageName,
                        coalesce(previous == null ? null : previous.getAppName(), discoveredName, packageName),
                        coalesce(previous == null ? null : previous.getDeveloperName(), developerName),
                        path
                );
                apps.put(packageName, app);
                if (previous == null || betterAppInfo(app, previous)) {
                    log("collected app: " + app.getPackageName() + " | "
                            + coalesce(app.getAppName(), app.getPackageName())
                            + " | " + coalesce(app.getDeveloperName(), path));
                }
            }

            currentUrl = HtmlParsers.extractNextUrl(body, currentUrl);
            pagesRead++;
        }

        List<AppInfo> sortedApps = new ArrayList<>(apps.values());
        for (int i = 0; i < sortedApps.size(); i++) {
            AppInfo app = sortedApps.get(i);
            if (app.getDeveloperName() == null && developerName != null) {
                sortedApps.set(i, new AppInfo(
                        app.getPackageName(),
                        app.getAppName(),
                        developerName,
                        app.getDeveloperPath()
                ));
            }
        }
        sortedApps.sort(Comparator
                .comparing((AppInfo app) -> coalesce(app.getAppName(), app.getPackageName()))
                .thenComparing(AppInfo::getPackageName));

        return new DeveloperApps(path, developerName, sortedApps);
    }

    private String fetch(String rawUrl) throws IOException, InterruptedException {
        String fullUrl = absolute(rawUrl);
        String cached = cache.get(fullUrl);
        if (cached != null) {
            return cached;
        }

        IOException lastError = null;
        for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
            throttler.awaitTurn();

            RuStorePageFetcher.PageResponse response;
            try {
                response = pageFetcher.fetch(URI.create(fullUrl));
            } catch (IOException exception) {
                lastError = exception;
                if (attempt < config.getMaxAttempts()) {
                    sleep(backoffDuration(attempt));
                    continue;
                }
                throw exception;
            }

            String body = response.getBody();
            int statusCode = response.getStatusCode();

            if (isRateLimited(statusCode, body)) {
                lastError = new IOException("rate limited on " + fullUrl);
                if (attempt < config.getMaxAttempts()) {
                    sleep(backoffDuration(attempt));
                    continue;
                }
                break;
            }

            if (statusCode != HttpURLConnectionCodes.HTTP_OK) {
                lastError = new IOException(fullUrl + " returned HTTP " + statusCode);
                if (shouldRetryStatus(statusCode) && attempt < config.getMaxAttempts()) {
                    sleep(backoffDuration(attempt));
                    continue;
                }
                throw lastError;
            }

            cache.put(fullUrl, body);
            return body;
        }

        if (lastError == null) {
            lastError = new IOException("request failed");
        }
        throw lastError;
    }

    private void sortApps(List<AppInfo> apps, List<MutableDeveloperSeed> developers) {
        Map<String, String> developersByPath = new HashMap<>();
        for (MutableDeveloperSeed developer : developers) {
            developersByPath.put(developer.path(), developer.name());
        }

        apps.sort((left, right) -> {
            String leftDeveloper = Objects.requireNonNullElse(
                    coalesce(left.getDeveloperName(), developersByPath.get(left.getDeveloperPath())),
                    ""
            );
            String rightDeveloper = Objects.requireNonNullElse(
                    coalesce(right.getDeveloperName(), developersByPath.get(right.getDeveloperPath())),
                    ""
            );
            int developerCompare = leftDeveloper.compareTo(rightDeveloper);
            if (developerCompare != 0) {
                return developerCompare;
            }

            String leftAppName = coalesce(left.getAppName(), left.getPackageName());
            String rightAppName = coalesce(right.getAppName(), right.getPackageName());
            int appCompare = leftAppName.compareTo(rightAppName);
            if (appCompare != 0) {
                return appCompare;
            }
            return left.getPackageName().compareTo(right.getPackageName());
        });
    }

    private List<DeveloperSeed> freezeDevelopers(List<MutableDeveloperSeed> developers) {
        List<DeveloperSeed> result = new ArrayList<>(developers.size());
        for (MutableDeveloperSeed developer : developers) {
            result.add(new DeveloperSeed(developer.id(), developer.path(), developer.name()));
        }
        return result;
    }

    private boolean shouldRetryStatus(int statusCode) {
        return statusCode == HttpURLConnectionCodes.HTTP_TOO_MANY_REQUESTS || statusCode >= 500;
    }

    private boolean isRateLimited(int statusCode, String body) {
        return statusCode == HttpURLConnectionCodes.HTTP_TOO_MANY_REQUESTS
                || body.contains("Ошибка 429");
    }

    private Duration backoffDuration(int attempt) {
        int normalizedAttempt = Math.max(attempt, 1);
        return Duration.ofSeconds((long) normalizedAttempt * normalizedAttempt);
    }

    private String absolute(String raw) {
        return HtmlParsers.absoluteWithBase(config.getBaseUrl(), raw);
    }

    private void sleep(Duration duration) throws InterruptedException {
        long millis = duration.toMillis();
        if (millis > 0L) {
            Thread.sleep(millis);
        }
    }

    private boolean betterAppInfo(AppInfo left, AppInfo right) {
        if (right.getAppName() == null && left.getAppName() != null) {
            return true;
        }
        return right.getDeveloperName() == null && left.getDeveloperName() != null;
    }

    private void log(String message) {
        RuStoreLogger logger = config.getLogger();
        if (logger != null) {
            logger.log(message);
        }
    }

    private void progress(String message) {
        RuStoreLogger progressLogger = config.getProgressLogger();
        if (progressLogger != null) {
            progressLogger.log(message);
        }
    }

    private static String developerLabel(MutableDeveloperSeed seed) {
        return Objects.requireNonNullElse(
                coalesce(seed.name(), seed.id(), seed.path()),
                "unknown"
        );
    }

    private static String coalesce(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private record DeveloperApps(String path, String name, List<AppInfo> apps) {
    }

    private record DeveloperCrawlResult(
            MutableDeveloperSeed seed,
            DeveloperApps data,
            IOException error
    ) {
    }

    private static final class MutableDeveloperSeed {
        private final String id;
        private final String path;
        private String name;

        private MutableDeveloperSeed(String id, String path) {
            this.id = id;
            this.path = path;
        }

        private String id() {
            return id;
        }

        private String path() {
            return path;
        }

        private String name() {
            return name;
        }

        private void name(String name) {
            this.name = name;
        }
    }

    private static final class HttpURLConnectionCodes {
        private static final int HTTP_OK = 200;
        private static final int HTTP_TOO_MANY_REQUESTS = 429;

        private HttpURLConnectionCodes() {
        }
    }
}
