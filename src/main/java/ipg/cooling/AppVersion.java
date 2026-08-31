package ipg.cooling;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppVersion {
    private static final String FALLBACK_VERSION = "1.0.1";
    private static final String FALLBACK_AUTHOR = "Veranika Biazruchanka";
    private static final Properties PROPS = load();

    private AppVersion() {
    }

    public static String display() {
        String version = PROPS.getProperty("version", "").trim();
        if (version.isEmpty() || version.contains("${")) {
            return FALLBACK_VERSION;
        }
        return version;
    }

    public static String author() {
        String author = PROPS.getProperty("author", "").trim();
        return author.isEmpty() ? FALLBACK_AUTHOR : author;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = AppVersion.class.getResourceAsStream("version.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ignored) {
            // fall back to defaults in display()/author()
        }
        return properties;
    }
}
