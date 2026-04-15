package wings.v.rustore.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class HttpPageFetcher implements RuStorePageFetcher {
    private static final String ACCEPT_HEADER = "text/html,application/xhtml+xml";

    private final Duration timeout;
    private final String userAgent;

    public HttpPageFetcher(Duration timeout, String userAgent) {
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
    }

    @Override
    public PageResponse fetch(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(Math.toIntExact(timeout.toMillis()));
        connection.setReadTimeout(Math.toIntExact(timeout.toMillis()));
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", ACCEPT_HEADER);

        try {
            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new PageResponse(statusCode, body);
        } finally {
            connection.disconnect();
        }
    }
}
