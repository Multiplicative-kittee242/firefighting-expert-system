import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Simple localization facade for Java 6 compatibility.
 * Uses custom UTF8Control to support readable Cyrillic in .properties files
 * (Java 6 PropertyResourceBundle defaults to ISO-8859-1).
 *
 * Static initialization at class load time.
 */
public final class Localization {
    private static final EnumMap<Language, String> mapFileByLanguage = new EnumMap<Language, String>(Language.class);
    static {
        for (Language language : Language.values()) {
            String mapFileName = String.format("map_%s.png", language.getValue());
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

        bundle = ResourceBundle.getBundle("i18n.messages", locale, new UTF8Control());
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

    /**
     * Java 6 workaround for UTF-8 .properties files.
     * Without this, Cyrillic in messages_ru.properties would be broken.
     */
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException
        {
            if (!"java.properties".equals(format))
                return super.newBundle(baseName, locale, format, loader, reload);

            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            InputStream stream = loader.getResourceAsStream(resourceName);

            // If a specific messages file not found (e.g. messages_en.properties) — fallback to messages.properties
            if (stream == null && !locale.getLanguage().isEmpty()) {
                // try to load a general messages file
                String defaultResourceName = toResourceName(baseName, "properties");
                stream = loader.getResourceAsStream(defaultResourceName);
            }
            if (stream == null)
                return null;

            try {
                return new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
            } finally {
                stream.close();
            }
        }
    }

    private enum Language {
        RU, EN, DE, NL;

        private final static Map<String, Language> valueToEnum = new HashMap<String, Language>();
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
