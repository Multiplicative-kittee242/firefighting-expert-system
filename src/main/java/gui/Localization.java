package gui;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Localization facade over classpath {@code i18n/messages*.properties}.
 * Property files are UTF-8; since Java 9 {@link ResourceBundle} loads them as UTF-8 by default
 * (JEP 226), so no custom {@code ResourceBundle.Control} is required.
 */
public final class Localization {
    private static final EnumMap<Language, String> mapFileByLanguage = new EnumMap<>(Language.class);

    static {
        for (Language language : Language.values()) {
            String mapFileName = String.format("images/map_%s.png", language.getValue());
            mapFileByLanguage.put(language, mapFileName);
        }
    }
    private static final Language DEFAULT_LANGUAGE = Language.EN;

    private static ResourceBundle bundle;

    private Localization() {}

    public static void init(Locale locale) {
        if (locale == null)
            locale = Locale.getDefault();

        locale = new Locale(locale.getLanguage()); // leave language only, without country

        System.setProperty("file.encoding", "UTF-8");

        String targetLanguage = locale.getLanguage();
        Language language = Language.findByValue(targetLanguage);
        if (language == null) {
            System.out.printf("Warning: Unsupported language '%s'. Falling back to %s.%n", targetLanguage, DEFAULT_LANGUAGE);
            locale = new Locale(DEFAULT_LANGUAGE.getValue());
        }
        ResourceBundle.clearCache();

        bundle = ResourceBundle.getBundle("i18n.messages", locale);
        System.out.println("Initialized localization with locale: " + bundle.getLocale());
    }

    public static String get(String key) {
        if (bundle == null)
            init(null);

        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public static String getMapImageFile() {
        if (bundle == null)
            init(null);

        String language = bundle.getLocale().getLanguage();
        String mapPath = mapFileByLanguage.get(Language.findByValue(language));
        return mapPath == null ? mapFileByLanguage.get(DEFAULT_LANGUAGE) : mapPath;
    }

    private enum Language {
        RU, EN, DE, NL;

        private static final Map<String, Language> valueToEnum = new HashMap<>();
        static {
            for (Language language : Language.values())
                valueToEnum.put(language.getValue(), language);
        }

        public static Language findByValue(String value) {
            return valueToEnum.get(value);
        }

        public String getValue() {
            return name().toLowerCase();
        }
    }
}
