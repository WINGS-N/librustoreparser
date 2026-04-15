package wings.v.rustore.parser;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

public interface RuStorePageFetcher {
    PageResponse fetch(URI uri) throws IOException;

    final class PageResponse {
        private final int statusCode;
        private final String body;

        public PageResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = Objects.requireNonNullElse(body, "");
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }
    }
}
