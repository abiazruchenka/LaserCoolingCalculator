package ipg.cooling.catalog;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON reader for the plate catalog. No third-party modules. */
final class CatalogJson {
    private final String text;
    private int index;

    private CatalogJson(String text) {
        this.text = text;
    }

    static Object read(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            builder.append(buffer, 0, read);
        }
        return new CatalogJson(builder.toString()).parseValue();
    }

    @SuppressWarnings("unchecked")
    static List<PlateDefinition> plates(Object root) {
        if (!(root instanceof Map<?, ?> map)) {
            return List.of();
        }
        Object raw = map.get("plates");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<PlateDefinition> plates = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> plate) {
                plates.add(plateOf((Map<String, Object>) plate));
            }
        }
        return plates;
    }

    private static PlateDefinition plateOf(Map<String, Object> map) {
        return new PlateDefinition(
                text(map, "id"),
                text(map, "name"),
                text(map, "drawing"),
                text(map, "ipgNumber"),
                text(map, "source"),
                text(map, "notes"),
                plateBody(object(map, "plate")),
                tubeBody(object(map, "tube")),
                layoutBody(object(map, "layout")),
                hydraulicsBody(object(map, "hydraulics"))
        );
    }

    private static PlateBody plateBody(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new PlateBody(
                text(map, "material"),
                number(map, "widthMm"),
                number(map, "heightMm"),
                number(map, "thicknessMm"),
                optionalNumber(map, "massKg"),
                bool(map, "massEstimated"),
                bool(map, "thicknessEstimated"),
                number(map, "conductivityWmk"),
                number(map, "specificHeatJkgK"),
                number(map, "edgeMarginMm")
        );
    }

    private static TubeBody tubeBody(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new TubeBody(
                text(map, "material"),
                text(map, "standard"),
                number(map, "innerDiameterMm"),
                number(map, "outerDiameterMm"),
                number(map, "wallThicknessMm"),
                optionalNumber(map, "massKg")
        );
    }

    private static LayoutBody layoutBody(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new LayoutBody(
                text(map, "orientation"),
                (int) Math.round(number(map, "passes")),
                number(map, "pitchMm"),
                number(map, "bendRadiusMm"),
                optionalNumber(map, "developedLengthMm")
        );
    }

    private static HydraulicsBody hydraulicsBody(Map<String, Object> map) {
        if (map == null) {
            return new HydraulicsBody(0, 0, false);
        }
        return new HydraulicsBody(
                number(map, "leakTestBarMin"),
                number(map, "leakTestBarMax"),
                bool(map, "leakTestIsOperatingPressure")
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map<?, ?> nested ? (Map<String, Object>) nested : null;
    }

    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static double number(Map<String, Object> map, String key) {
        Double value = optionalNumber(map, key);
        return value == null ? 0.0 : value;
    }

    private static Double optionalNumber(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private static boolean bool(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Boolean flag && flag;
    }

    private Object parseValue() {
        skipSpace();
        if (index >= text.length()) {
            throw new IllegalArgumentException("empty json");
        }
        char ch = text.charAt(index);
        if (ch == '{') {
            return parseObject();
        }
        if (ch == '[') {
            return parseArray();
        }
        if (ch == '"') {
            return parseString();
        }
        if (ch == 't' || ch == 'f' || ch == 'n') {
            return parseLiteral();
        }
        return parseNumber();
    }

    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        skipSpace();
        if (peek('}')) {
            index++;
            return map;
        }
        while (true) {
            skipSpace();
            String key = parseString();
            skipSpace();
            expect(':');
            map.put(key, parseValue());
            skipSpace();
            if (peek('}')) {
                index++;
                return map;
            }
            expect(',');
        }
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipSpace();
        if (peek(']')) {
            index++;
            return list;
        }
        while (true) {
            list.add(parseValue());
            skipSpace();
            if (peek(']')) {
                index++;
                return list;
            }
            expect(',');
        }
    }

    private String parseString() {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (index < text.length()) {
            char ch = text.charAt(index++);
            if (ch == '"') {
                return builder.toString();
            }
            if (ch == '\\' && index < text.length()) {
                char escaped = text.charAt(index++);
                builder.append(switch (escaped) {
                    case '"', '\\', '/' -> escaped;
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> escaped;
                });
            } else {
                builder.append(ch);
            }
        }
        throw new IllegalArgumentException("unterminated string");
    }

    private Object parseLiteral() {
        if (text.startsWith("true", index)) {
            index += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", index)) {
            index += 5;
            return Boolean.FALSE;
        }
        if (text.startsWith("null", index)) {
            index += 4;
            return null;
        }
        throw new IllegalArgumentException("bad token at " + index);
    }

    private Number parseNumber() {
        int start = index;
        if (peek('-')) {
            index++;
        }
        while (index < text.length() && (Character.isDigit(text.charAt(index)) || text.charAt(index) == '.'
                || text.charAt(index) == 'e' || text.charAt(index) == 'E'
                || text.charAt(index) == '+' || text.charAt(index) == '-')) {
            if (text.charAt(index) == '+' || (text.charAt(index) == '-' && index > start)) {
                char prev = text.charAt(index - 1);
                if (prev != 'e' && prev != 'E') {
                    break;
                }
            }
            index++;
        }
        return Double.parseDouble(text.substring(start, index));
    }

    private void skipSpace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }

    private boolean peek(char expected) {
        return index < text.length() && text.charAt(index) == expected;
    }

    private void expect(char expected) {
        skipSpace();
        if (!peek(expected)) {
            throw new IllegalArgumentException("expected '" + expected + "' at " + index);
        }
        index++;
    }
}
