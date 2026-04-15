package wings.v.rustore.parser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RuStoreCrawlerConfig {
    private final String baseUrl;
    private final List<String> seedDeveloperIds;
    private final List<String> directPackages;
    private final int maxDeveloperPages;
    private final int maxSeedDevelopers;
    private final int concurrency;
    private final Duration requestTimeout;
    private final Duration requestDelay;
    private final int maxAttempts;
    private final String userAgent;
    private final RuStoreLogger logger;

    private RuStoreCrawlerConfig(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.seedDeveloperIds = immutableCopy(builder.seedDeveloperIds);
        this.directPackages = immutableCopy(builder.directPackages);
        this.maxDeveloperPages = builder.maxDeveloperPages;
        this.maxSeedDevelopers = builder.maxSeedDevelopers;
        this.concurrency = builder.concurrency;
        this.requestTimeout = builder.requestTimeout;
        this.requestDelay = builder.requestDelay;
        this.maxAttempts = builder.maxAttempts;
        this.userAgent = builder.userAgent;
        this.logger = builder.logger;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public List<String> getSeedDeveloperIds() {
        return seedDeveloperIds;
    }

    public List<String> getDirectPackages() {
        return directPackages;
    }

    public int getMaxDeveloperPages() {
        return maxDeveloperPages;
    }

    public int getMaxSeedDevelopers() {
        return maxSeedDevelopers;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public Duration getRequestDelay() {
        return requestDelay;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public RuStoreLogger getLogger() {
        return logger;
    }

    private static List<String> immutableCopy(Collection<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    public static final class Builder {
        private String baseUrl = RuStoreDefaults.DEFAULT_BASE_URL;
        private List<String> seedDeveloperIds = RuStoreDefaults.getDefaultSuspiciousSeedDeveloperIds();
        private List<String> directPackages = RuStoreDefaults.getDefaultSuspiciousDirectPackages();
        private int maxDeveloperPages;
        private int maxSeedDevelopers;
        private int concurrency = 3;
        private Duration requestTimeout = Duration.ofSeconds(20);
        private Duration requestDelay = Duration.ofMillis(300);
        private int maxAttempts = 5;
        private String userAgent = RuStoreDefaults.DEFAULT_USER_AGENT;
        private RuStoreLogger logger;

        private Builder() {
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = requireText(baseUrl, "baseUrl");
            return this;
        }

        public Builder seedDeveloperIds(Collection<String> seedDeveloperIds) {
            this.seedDeveloperIds = cleanTextList(seedDeveloperIds, "seedDeveloperIds");
            return this;
        }

        public Builder directPackages(Collection<String> directPackages) {
            this.directPackages = cleanTextList(directPackages, "directPackages");
            return this;
        }

        public Builder maxDeveloperPages(int maxDeveloperPages) {
            if (maxDeveloperPages < 0) {
                throw new IllegalArgumentException("maxDeveloperPages must be >= 0");
            }
            this.maxDeveloperPages = maxDeveloperPages;
            return this;
        }

        public Builder maxSeedDevelopers(int maxSeedDevelopers) {
            if (maxSeedDevelopers < 0) {
                throw new IllegalArgumentException("maxSeedDevelopers must be >= 0");
            }
            this.maxSeedDevelopers = maxSeedDevelopers;
            return this;
        }

        public Builder concurrency(int concurrency) {
            if (concurrency < 1) {
                throw new IllegalArgumentException("concurrency must be >= 1");
            }
            this.concurrency = concurrency;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = positiveDuration(requestTimeout, "requestTimeout");
            return this;
        }

        public Builder requestDelay(Duration requestDelay) {
            this.requestDelay = nonNegativeDuration(requestDelay, "requestDelay");
            return this;
        }

        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = requireText(userAgent, "userAgent");
            return this;
        }

        public Builder logger(RuStoreLogger logger) {
            this.logger = logger;
            return this;
        }

        public RuStoreCrawlerConfig build() {
            if (seedDeveloperIds.isEmpty()) {
                throw new IllegalArgumentException("seedDeveloperIds must not be empty");
            }
            return new RuStoreCrawlerConfig(this);
        }

        private static Duration positiveDuration(Duration value, String fieldName) {
            Objects.requireNonNull(value, fieldName);
            if (value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(fieldName + " must be > 0");
            }
            return value;
        }

        private static Duration nonNegativeDuration(Duration value, String fieldName) {
            Objects.requireNonNull(value, fieldName);
            if (value.isNegative()) {
                throw new IllegalArgumentException(fieldName + " must be >= 0");
            }
            return value;
        }

        private static String requireText(String value, String fieldName) {
            Objects.requireNonNull(value, fieldName);
            String normalized = value.trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
            return normalized;
        }

        private static List<String> cleanTextList(Collection<String> values, String fieldName) {
            Objects.requireNonNull(values, fieldName);
            List<String> result = new ArrayList<>(values.size());
            for (String value : values) {
                String normalized = requireText(value, fieldName + " item");
                if (!result.contains(normalized)) {
                    result.add(normalized);
                }
            }
            return result;
        }
    }
}
