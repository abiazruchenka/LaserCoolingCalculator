package ipg.cooling;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders T_in, P_out, c_p as an italic letter plus a roman subscript.
 */
public final class Formula {
    private static final Pattern TOKEN = Pattern.compile("([A-Za-zΑ-Ωα-ωΔλρνṁ]|Nu|Δ[PT])_([A-Za-z]+)");

    private Formula() {
    }

    public static TextFlow flow(String source) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("chart-formula");
        set(flow, source);
        return flow;
    }

    public static void set(TextFlow flow, String source) {
        flow.getChildren().setAll(nodes(source == null ? "" : source));
    }

    public static String compact(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        Matcher matcher = TOKEN.matcher(source);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            out.append(source, last, matcher.start());
            out.append(matcher.group(1));
            String sub = unicodeSub(matcher.group(2));
            out.append(sub != null ? sub : "_" + matcher.group(2));
            last = matcher.end();
        }
        out.append(source.substring(last));
        return out.toString();
    }

    private static List<Text> nodes(String source) {
        if (source.matches("V̇|[A-Za-zΑ-Ωα-ωΔλρνṁητα]|Nu|Re|Δ[PT]")) {
            return List.of(letter(source));
        }
        List<Text> nodes = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(source);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                nodes.add(plain(source.substring(last, matcher.start())));
            }
            nodes.add(letter(matcher.group(1)));
            nodes.add(sub(matcher.group(2)));
            last = matcher.end();
        }
        if (last < source.length()) {
            nodes.add(plain(source.substring(last)));
        }
        return nodes;
    }

    private static Text letter(String value) {
        Text text = new Text(value);
        text.getStyleClass().add("formula-var");
        return text;
    }

    private static Text sub(String index) {
        Text text = new Text(index);
        text.getStyleClass().add("formula-sub");
        text.setTranslateY(5);
        return text;
    }

    private static Text plain(String value) {
        Text text = new Text(value);
        text.getStyleClass().add("formula-plain");
        return text;
    }

    private static String unicodeSub(String index) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < index.length(); i++) {
            char mapped = subscript(index.charAt(i));
            if (mapped == 0) {
                return null;
            }
            out.append(mapped);
        }
        return out.toString();
    }

    private static char subscript(char ch) {
        return switch (ch) {
            case 'a' -> 'ₐ';
            case 'e' -> 'ₑ';
            case 'h' -> 'ₕ';
            case 'i' -> 'ᵢ';
            case 'j' -> 'ⱼ';
            case 'k' -> 'ₖ';
            case 'l' -> 'ₗ';
            case 'm' -> 'ₘ';
            case 'n' -> 'ₙ';
            case 'o' -> 'ₒ';
            case 'p' -> 'ₚ';
            case 'r' -> 'ᵣ';
            case 's' -> 'ₛ';
            case 't' -> 'ₜ';
            case 'u' -> 'ᵤ';
            case 'v' -> 'ᵥ';
            case 'x' -> 'ₓ';
            default -> 0;
        };
    }
}
