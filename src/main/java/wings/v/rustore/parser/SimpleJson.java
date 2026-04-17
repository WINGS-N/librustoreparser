package wings.v.rustore.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJson {
    private SimpleJson() {
    }

    static Object parse(String json) {
        return new Parser(json).parseValue();
    }

    static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value);
        return builder.toString();
    }

    private static void writeValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String string) {
            writeString(builder, string);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeString(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                writeValue(builder, entry.getValue());
            }
            builder.append('}');
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeValue(builder, item);
            }
            builder.append(']');
            return;
        }
        throw new IllegalArgumentException("Unsupported JSON type: " + value.getClass());
    }

    private static void writeString(StringBuilder builder, String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source == null ? "" : source;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= source.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON input");
            }
            char current = source.charAt(index);
            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }
                    throw new IllegalArgumentException("Unexpected JSON token at index " + index);
                }
            };
        }

        private Map<String, Object> parseObject() {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            List<Object> result = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < source.length()) {
                char current = source.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current != '\\') {
                    builder.append(current);
                    continue;
                }
                if (index >= source.length()) {
                    throw new IllegalArgumentException("Dangling escape sequence");
                }
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> builder.append(escaped);
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (index + 4 > source.length()) {
                            throw new IllegalArgumentException("Invalid unicode escape");
                        }
                        String hex = source.substring(index, index + 4);
                        builder.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                    }
                    default -> throw new IllegalArgumentException("Unsupported escape sequence: \\" + escaped);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private Object parseLiteral(String literal, Object value) {
            if (!source.startsWith(literal, index)) {
                throw new IllegalArgumentException("Expected " + literal + " at index " + index);
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            if (source.charAt(index) == '-') {
                index++;
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            if (index < source.length() && source.charAt(index) == '.') {
                index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
                return Double.parseDouble(source.substring(start, index));
            }
            return Long.parseLong(source.substring(start, index));
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean peek(char expected) {
            return index < source.length() && source.charAt(index) == expected;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= source.length() || source.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at index " + index);
            }
            index++;
        }
    }
}
