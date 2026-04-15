package wings.v.rustore.parser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HtmlParsers {
    private static final Pattern APP_PAIR_PATTERN = Pattern.compile(
            "\"packageName\":\"((?:\\\\.|[^\"\\\\])+)\",\"appName\":\"((?:\\\\.|[^\"\\\\])*)\""
    );
    private static final Pattern APP_HREF_PATTERN = Pattern.compile("href=\"/catalog/app/([^\"?#]+)\"");
    private static final Pattern NEXT_LINK_HREF_PATTERN = Pattern.compile("<link rel=\"next\" href=\"([^\"]+)\"");
    private static final Pattern NEXT_ANCHOR_HREF_PATTERN =
            Pattern.compile("<a[^>]*data-testid=\"next\"[^>]*href=\"([^\"]+)\"");
    private static final Pattern NEXT_ANCHOR_HREF_ALT_PATTERN =
            Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*data-testid=\"next\"");
    private static final Pattern PAGE_H1_PATTERN = Pattern.compile("<h1[^>]*>([^<]+)</h1>");
    private static final Pattern APP_DEVELOPER_AUTHOR_ID_PATTERN = Pattern.compile(
            "author\":\\{\"@id\":\"https://www\\.rustore\\.ru(/catalog/developer/[^\"]+)\""
    );
    private static final Pattern APP_DEVELOPER_HREF_PATTERN =
            Pattern.compile("href=\"(/catalog/developer/[^\"?#]+)\"");
    private static final Pattern APP_DEVELOPER_ORG_NAME_PATTERN = Pattern.compile(
            "\\{\"@type\":\"Organization\",\"@id\":\"https://www\\.rustore\\.ru/catalog/developer/[^\"]+\""
                    + "(?:,\"url\":\"[^\"]+\")?,\"name\":\"((?:\\\\.|[^\"\\\\])+)\""
    );
    private static final Pattern APP_SCOPED_ORG_NAME_PATTERN = Pattern.compile(
            "\\{\"@type\":\"Organization\",\"@id\":\"https://www\\.rustore\\.ru/catalog/app/[^\"]+#dev\","
                    + "\"name\":\"((?:\\\\.|[^\"\\\\])+)\""
    );

    private HtmlParsers() {
    }

    static Map<String, String> extractApps(String body) {
        Map<String, String> apps = new LinkedHashMap<>();

        Matcher pairMatcher = APP_PAIR_PATTERN.matcher(body);
        while (pairMatcher.find()) {
            String packageName = decodeJsonFragment(pairMatcher.group(1));
            String appName = decodeJsonFragment(pairMatcher.group(2));
            if (packageName.isEmpty()) {
                continue;
            }
            apps.putIfAbsent(packageName, coalesce(appName, packageName));
        }

        Matcher hrefMatcher = APP_HREF_PATTERN.matcher(body);
        while (hrefMatcher.find()) {
            String packageName = cleanText(hrefMatcher.group(1));
            if (!packageName.isEmpty()) {
                apps.putIfAbsent(packageName, packageName);
            }
        }

        return apps;
    }

    static String extractNextUrl(String body, String currentUrl) {
        for (Pattern pattern : new Pattern[]{
                NEXT_LINK_HREF_PATTERN,
                NEXT_ANCHOR_HREF_PATTERN,
                NEXT_ANCHOR_HREF_ALT_PATTERN
        }) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                return resolveUrl(currentUrl, HtmlEntityDecoder.unescape(matcher.group(1)));
            }
        }
        return null;
    }

    static String extractPageH1(String body) {
        Matcher matcher = PAGE_H1_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return cleanText(matcher.group(1));
    }

    static String extractDeveloperName(String body) {
        return extractPageH1(body);
    }

    static String extractAppDeveloperPath(String body) {
        for (Pattern pattern : new Pattern[]{APP_DEVELOPER_AUTHOR_ID_PATTERN, APP_DEVELOPER_HREF_PATTERN}) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                return ensurePath(matcher.group(1));
            }
        }
        return null;
    }

    static String extractAppCompanyName(String body) {
        for (Pattern pattern : new Pattern[]{APP_DEVELOPER_ORG_NAME_PATTERN, APP_SCOPED_ORG_NAME_PATTERN}) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                return cleanText(decodeJsonFragment(matcher.group(1)));
            }
        }
        return null;
    }

    static String decodeJsonFragment(String fragment) {
        if (fragment == null || fragment.isEmpty()) {
            return "";
        }

        try {
            return HtmlEntityDecoder.unescape(JsonStringDecoder.decode(fragment));
        } catch (IllegalArgumentException exception) {
            return HtmlEntityDecoder.unescape(fragment);
        }
    }

    static String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return HtmlEntityDecoder.unescape(value).trim();
    }

    static String resolveUrl(String baseUrl, String ref) {
        try {
            URI base = new URI(baseUrl);
            if (ref.startsWith("?")) {
                return new URI(base.getScheme(), base.getAuthority(), base.getPath(), ref.substring(1), null)
                        .toString();
            }
            return base.resolve(ref).toString();
        } catch (URISyntaxException exception) {
            return ref;
        }
    }

    static String absoluteWithBase(String base, String raw) {
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }
        String normalizedBase = stripTrailingSlash(base);
        return normalizedBase + ensurePath(raw);
    }

    static String ensurePath(String raw) {
        if (raw.startsWith("/")) {
            return raw;
        }
        return "/" + raw;
    }

    private static String stripTrailingSlash(String value) {
        String current = value;
        while (current.endsWith("/")) {
            current = current.substring(0, current.length() - 1);
        }
        return current;
    }

    private static String coalesce(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static final class JsonStringDecoder {
        private JsonStringDecoder() {
        }

        private static String decode(String fragment) {
            StringBuilder result = new StringBuilder(fragment.length());
            for (int i = 0; i < fragment.length(); i++) {
                char current = fragment.charAt(i);
                if (current != '\\') {
                    result.append(current);
                    continue;
                }
                if (i + 1 >= fragment.length()) {
                    throw new IllegalArgumentException("dangling escape");
                }

                char escaped = fragment.charAt(++i);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        result.append(escaped);
                        break;
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'u':
                        if (i + 4 >= fragment.length()) {
                            throw new IllegalArgumentException("bad unicode escape");
                        }
                        String codePoint = fragment.substring(i + 1, i + 5);
                        result.append((char) Integer.parseInt(codePoint, 16));
                        i += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("unsupported escape: " + escaped);
                }
            }
            return result.toString();
        }
    }

    private static final class HtmlEntityDecoder {
        private static final Map<String, String> NAMED_ENTITIES = buildNamedEntities();

        private HtmlEntityDecoder() {
        }

        private static String unescape(String value) {
            if (value == null || value.indexOf('&') < 0) {
                return value;
            }

            StringBuilder result = new StringBuilder(value.length());
            int cursor = 0;
            while (cursor < value.length()) {
                int ampersand = value.indexOf('&', cursor);
                if (ampersand < 0) {
                    result.append(value, cursor, value.length());
                    break;
                }

                result.append(value, cursor, ampersand);
                int semicolon = value.indexOf(';', ampersand + 1);
                if (semicolon < 0) {
                    result.append(value, ampersand, value.length());
                    break;
                }

                String entity = value.substring(ampersand + 1, semicolon);
                String decoded = decodeEntity(entity);
                if (decoded == null) {
                    result.append(value, ampersand, semicolon + 1);
                } else {
                    result.append(decoded);
                }
                cursor = semicolon + 1;
            }

            return result.toString();
        }

        private static String decodeEntity(String entity) {
            if (entity.startsWith("#x") || entity.startsWith("#X")) {
                try {
                    int codePoint = Integer.parseInt(entity.substring(2), 16);
                    return new String(Character.toChars(codePoint));
                } catch (IllegalArgumentException exception) {
                    return null;
                }
            }
            if (entity.startsWith("#")) {
                try {
                    int codePoint = Integer.parseInt(entity.substring(1));
                    return new String(Character.toChars(codePoint));
                } catch (IllegalArgumentException exception) {
                    return null;
                }
            }
            return NAMED_ENTITIES.get(entity);
        }

        private static Map<String, String> buildNamedEntities() {
            Map<String, String> entities = new HashMap<>();
            entities.put("amp", "&");
            entities.put("lt", "<");
            entities.put("gt", ">");
            entities.put("quot", "\"");
            entities.put("apos", "'");
            entities.put("#39", "'");
            entities.put("nbsp", "\u00A0");
            return entities;
        }
    }
}
