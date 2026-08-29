package ipg.cooling;

import java.util.Locale;

public enum AppLanguage {
    RU(Locale.of("ru"), "Русский", "RU"),
    DE(Locale.GERMAN, "Deutsch", "DE"),
    EN(Locale.ENGLISH, "English", "EN");

    private final Locale locale;
    private final String displayName;
    private final String code;

    AppLanguage(Locale locale, String displayName, String code) {
        this.locale = locale;
        this.displayName = displayName;
        this.code = code;
    }

    public Locale locale() {
        return locale;
    }

    public String displayName() {
        return displayName;
    }

    public String code() {
        return code;
    }

    public static AppLanguage detect() {
        String language = Locale.getDefault().getLanguage();
        if (Locale.GERMAN.getLanguage().equals(language)) {
            return DE;
        }
        if (Locale.ENGLISH.getLanguage().equals(language)) {
            return EN;
        }
        return RU;
    }
}
