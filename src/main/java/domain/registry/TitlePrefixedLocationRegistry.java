package domain.registry;

import domain.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared shell for registries keyed by lower-cased titles of the form {@code <prefix><locationCode><optionalSuffix>}
 * (e.g. {@code hydr_d1}, {@code est_a}): normalize title → derive owning location from the character after the prefix →
 * store by title.
 * <p>
 * Package-private: only {@link HydrantOutletsRegistry} and {@link ExtinguisherRegistry} share this convention today;
 * other keyed registries use different key spaces (codes, upper case, no prefix).
 *
 * @param <T> registered value type
 */
abstract class TitlePrefixedLocationRegistry<T> {
    private final Map<String, T> byTitle = new LinkedHashMap<>();
    private final List<T> all = new ArrayList<>();
    private final String titlePrefix;
    private final String kindName;

    /**
     * @param titlePrefix lower-case prefix including trailing underscore, e.g. {@code "hydr_"} @param kindName
     * lower-case noun for error messages, e.g. {@code "hydrant "}
     */
    protected TitlePrefixedLocationRegistry(String titlePrefix, String kindName) {
        this.titlePrefix = Objects.requireNonNull(titlePrefix, "titlePrefix");
        this.kindName = Objects.requireNonNull(kindName, "kindName");
    }

    /**
     * Builds entries from a title → attribute map. Blank titles are skipped; duplicate normalized titles fail fast;
     * malformed titles / unknown locations throw from {@link #deriveLocationCode}/{@link LocationRegistry#get}.
     *
     * @param rawByTitle titles as authored (any case) → attribute carried on the value object @param locations registry
     * used to resolve the owning location code @param factory builds {@code T} from normalized title, resolved
     * location, and attribute @param <A> attribute type (outlet count, extinguisher type, …)
     */
    protected final <A> void buildFromTitleMap(Map<String, A> rawByTitle, LocationRegistry locations,
        ItemFactory<A, T> factory)
    {
        for (Map.Entry<String, A> entry : rawByTitle.entrySet()) {
            String raw = entry.getKey();
            if (raw != null && !raw.isBlank()) {
                String normalized = normalizeTitle(raw);
                Location location = resolveOwningLocation(normalized, locations);
                T value = factory.create(normalized, location, entry.getValue());
                register(raw, normalized, value);
            }
        }
    }

    private void register(String rawInputTitle, String normalizedTitle, T value) {
        if (byTitle.putIfAbsent(normalizedTitle, value) != null)
            throw new IllegalStateException("Duplicate " + kindName + " title: " + rawInputTitle);
        all.add(value);
    }

    private static String normalizeTitle(String rawTitle) {
        return rawTitle.trim().toLowerCase();
    }

    private Location resolveOwningLocation(String normalizedTitle, LocationRegistry locations) {
        return locations.get(deriveLocationCode(normalizedTitle));
    }

    private String deriveLocationCode(String normalizedTitle) {
        if (!normalizedTitle.startsWith(titlePrefix) || normalizedTitle.length() <= titlePrefix.length()) {
            throw new IllegalArgumentException(capitalize(kindName) + " title must start with '" + titlePrefix
                + "' followed by a location code: " + normalizedTitle);
        }
        return normalizedTitle.substring(titlePrefix.length(), titlePrefix.length() + 1);
    }

    private static String capitalize(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * Resolves a title (any case) to its value, failing if unknown.
     */
    public final T get(String title) {
        T result = find(title);
        if (result == null)
            throw new IllegalArgumentException("Unknown " + kindName + " title: " + title);
        return result;
    }

    public final boolean exists(String title) {
        return find(title) != null;
    }

    /**
     * @return an unmodifiable view of all registered values, in insertion order.
     */
    public final List<T> all() {
        return Collections.unmodifiableList(all);
    }

    private T find(String title) {
        return (title == null || title.isBlank()) ? null : byTitle.get(title.trim().toLowerCase());
    }

    /**
     * Creates one registry value from a normalized title, its owning location, and the map attribute.
     *
     * @param <A> attribute type from the input map @param <T> registry value type
     */
    @FunctionalInterface
    protected interface ItemFactory<A, T> {
        T create(String normalizedTitle, Location location, A attribute);
    }
}
