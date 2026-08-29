package ipg.cooling;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class I18n {
    private static final String BUNDLE = "ipg.cooling.messages";

    private static AppLanguage language = AppLanguage.detect();
    private static ResourceBundle bundle = load(language);

    private I18n() {
    }

    public static void setLanguage(AppLanguage next) {
        language = next;
        bundle = load(next);
    }

    public static AppLanguage language() {
        return language;
    }

    public static Locale locale() {
        return language.locale();
    }

    public static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    public static String t(String key, Object... args) {
        MessageFormat format = new MessageFormat(t(key), locale());
        return format.format(args);
    }

    private static ResourceBundle load(AppLanguage language) {
        return ResourceBundle.getBundle(BUNDLE, language.locale());
    }
}
