package wings.v.rustore.parser;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RuStoreCrawlerCli {
    private RuStoreCrawlerCli() {
    }

    public static void main(String[] args) throws Exception {
        CliOptions options = CliOptions.parse(args);
        if (options.help()) {
            printUsage();
            return;
        }

        RuStoreCrawlerConfig.Builder configBuilder = RuStoreCrawlerConfig.builder()
                .maxDeveloperPages(options.maxDeveloperPages())
                .maxSeedDevelopers(options.maxSeedDevelopers())
                .concurrency(options.concurrency())
                .requestTimeout(Duration.ofSeconds(options.timeoutSeconds()))
                .requestDelay(Duration.ofMillis(options.requestDelayMillis()))
                .maxAttempts(options.maxAttempts());

        if (options.verbose()) {
            configBuilder.logger(message -> System.err.println("[librustoreparser] " + message));
        }
        if (options.progress() || options.verbose()) {
            configBuilder.progressLogger(message -> System.err.println("[librustoreparser] " + message));
        }
        if (!options.seedDeveloperIds().isEmpty()) {
            configBuilder.seedDeveloperIds(options.seedDeveloperIds());
        }
        if (!options.directPackages().isEmpty()) {
            configBuilder.directPackages(options.directPackages());
        }

        RuStoreCrawler crawler = new RuStoreCrawler(configBuilder.build());
        CrawlResult result = crawl(crawler);

        if (options.full()) {
            printFull(result);
        } else {
            printPackages(result);
        }
    }

    private static CrawlResult crawl(RuStoreCrawler crawler) throws IOException, InterruptedException {
        return crawler.crawl();
    }

    private static void printPackages(CrawlResult result) {
        for (String packageName : result.getPackageNames()) {
            System.out.println(packageName);
        }
    }

    private static void printFull(CrawlResult result) {
        for (AppInfo app : result.getApps()) {
            System.out.println(app.getPackageName()
                    + "\t" + nullable(app.getAppName())
                    + "\t" + nullable(app.getDeveloperName())
                    + "\t" + nullable(app.getDeveloperPath()));
        }
    }

    private static String nullable(String value) {
        return value == null ? "" : value;
    }

    private static void printUsage() {
        System.out.println("Usage: ./gradlew -p external/librustoreparser runCrawlerCli --args='[options]'");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --help                         Show this help and exit.");
        System.out.println("  --progress                     Print concise crawl progress to stderr.");
        System.out.println("  --verbose                      Print crawler progress to stderr.");
        System.out.println("  --full                         Print package, app name, developer name, developer path.");
        System.out.println("  --developer-pages=N            Max pages per developer. 0 = all. Default: 0.");
        System.out.println("  --max-seed-developers=N        Limit number of seed developers. 0 = all. Default: 0.");
        System.out.println("  --concurrency=N                Parallel developer crawls. Default: 3.");
        System.out.println("  --timeout-seconds=N            HTTP timeout in seconds. Default: 20.");
        System.out.println("  --request-delay-ms=N           Delay between requests in milliseconds. Default: 300.");
        System.out.println("  --max-attempts=N               Retry attempts per request. Default: 5.");
        System.out.println("  --seed-developers=id1,id2      Override default seed developer IDs.");
        System.out.println("  --direct-packages=p1,p2        Override default direct package list.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  ./gradlew -p external/librustoreparser runCrawlerCli --args='--max-seed-developers=1 --verbose'");
        System.out.println("  ./gradlew -p external/librustoreparser runCrawlerCli --args='--seed-developers=df91b73f --full'");
    }

    private record CliOptions(
            boolean help,
            boolean progress,
            boolean verbose,
            boolean full,
            int maxDeveloperPages,
            int maxSeedDevelopers,
            int concurrency,
            int timeoutSeconds,
            int requestDelayMillis,
            int maxAttempts,
            List<String> seedDeveloperIds,
            List<String> directPackages
    ) {
        private static CliOptions parse(String[] args) {
            boolean help = false;
            boolean progress = false;
            boolean verbose = false;
            boolean full = false;
            int maxDeveloperPages = 0;
            int maxSeedDevelopers = 0;
            int concurrency = 3;
            int timeoutSeconds = 20;
            int requestDelayMillis = 300;
            int maxAttempts = 5;
            List<String> seedDeveloperIds = List.of();
            List<String> directPackages = List.of();

            for (String arg : args) {
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    help = true;
                } else if ("--progress".equals(arg)) {
                    progress = true;
                } else if ("--verbose".equals(arg) || "-v".equals(arg)) {
                    verbose = true;
                } else if ("--full".equals(arg)) {
                    full = true;
                } else if (arg.startsWith("--developer-pages=")) {
                    maxDeveloperPages = parseNonNegativeInt(arg, "--developer-pages=");
                } else if (arg.startsWith("--max-seed-developers=")) {
                    maxSeedDevelopers = parseNonNegativeInt(arg, "--max-seed-developers=");
                } else if (arg.startsWith("--concurrency=")) {
                    concurrency = parsePositiveInt(arg, "--concurrency=");
                } else if (arg.startsWith("--timeout-seconds=")) {
                    timeoutSeconds = parsePositiveInt(arg, "--timeout-seconds=");
                } else if (arg.startsWith("--request-delay-ms=")) {
                    requestDelayMillis = parseNonNegativeInt(arg, "--request-delay-ms=");
                } else if (arg.startsWith("--max-attempts=")) {
                    maxAttempts = parsePositiveInt(arg, "--max-attempts=");
                } else if (arg.startsWith("--seed-developers=")) {
                    seedDeveloperIds = parseCsv(arg, "--seed-developers=");
                } else if (arg.startsWith("--direct-packages=")) {
                    directPackages = parseCsv(arg, "--direct-packages=");
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            return new CliOptions(
                    help,
                    progress,
                    verbose,
                    full,
                    maxDeveloperPages,
                    maxSeedDevelopers,
                    concurrency,
                    timeoutSeconds,
                    requestDelayMillis,
                    maxAttempts,
                    seedDeveloperIds,
                    directPackages
            );
        }

        private static int parsePositiveInt(String rawArg, String prefix) {
            int value = parseInt(rawArg, prefix);
            if (value < 1) {
                throw new IllegalArgumentException(prefix + " value must be >= 1");
            }
            return value;
        }

        private static int parseNonNegativeInt(String rawArg, String prefix) {
            int value = parseInt(rawArg, prefix);
            if (value < 0) {
                throw new IllegalArgumentException(prefix + " value must be >= 0");
            }
            return value;
        }

        private static int parseInt(String rawArg, String prefix) {
            String rawValue = rawArg.substring(prefix.length()).trim();
            try {
                return Integer.parseInt(rawValue);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Bad integer for " + prefix + ": " + rawValue, exception);
            }
        }

        private static List<String> parseCsv(String rawArg, String prefix) {
            String rawValue = rawArg.substring(prefix.length()).trim();
            if (rawValue.isEmpty()) {
                return List.of();
            }

            List<String> values = new ArrayList<>();
            for (String part : Arrays.asList(rawValue.split(","))) {
                String normalized = part.trim();
                if (!normalized.isEmpty() && !values.contains(normalized)) {
                    values.add(normalized);
                }
            }
            return values;
        }
    }
}
