package domain.registry;

import domain.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared shell for registries keyed by upper-cased two-character endpoint codes (e.g. {@code "AB "}, {@code "QA "}):
 * validate length, resolve both endpoints against a {@link LocationRegistry}, reject self-loops, then let the subclass
 * decide endpoint order and build the value object.
 * <p>
 * Package-private: only {@link LinkRegistry} (undirected, alphabetically ordered ends) and
 * {@link EvacuationRouteRegistry} (directed, input order preserved) share this scheme.
 *
 * @param <T> registered value type
 */
abstract class TwoCharEndpointRegistry<T> {
    private final Map<String, T> byCode = new LinkedHashMap<>();
    private final List<T> all = new ArrayList<>();
    private final String kindName;

    /**
     * @param kindName lower-case noun phrase for error messages, e.g. {@code "link "} or {@code "evacuation route "}
     */
    protected TwoCharEndpointRegistry(String kindName) {
        this.kindName = Objects.requireNonNull(kindName, "kindName");
    }

    /**
     * Builds entries from raw two-character codes. Null / blank codes are skipped; duplicates fail fast; malformed
     * codes / unknown endpoints / self-loops throw.
     */
    protected final void buildFromCodes(List<String> rawCodes, LocationRegistry locations, ItemFactory<T> factory) {
        for (String raw : rawCodes) {
            if (raw != null && !raw.isBlank()) {
                ParsedEndpoints parsed = parseEndpoints(raw, locations);
                T value = factory.create(parsed.code(), parsed.from(), parsed.to());
                register(raw, parsed.code(), value);
            }
        }
    }

    /**
     * Orders the two resolved endpoints for the value object. {@code c1}/{@code loc1} are the first character of the
     * upper-cased code; {@code c2}/{@code loc2} the second.
     */
    protected abstract OrderedEndpoints orderEndpoints(String c1, String c2, Location loc1, Location loc2);

    private ParsedEndpoints parseEndpoints(String rawCode, LocationRegistry locations) {
        String upperCode = rawCode.trim().toUpperCase();
        if (upperCode.length() != 2)
            throw new IllegalArgumentException(capitalize(kindName) + " code must be exactly 2 characters: " + rawCode);

        String c1 = upperCode.substring(0, 1);
        String c2 = upperCode.substring(1, 2);
        Location loc1 = locations.get(c1);
        Location loc2 = locations.get(c2);

        if (loc1.equals(loc2))
            throw new IllegalArgumentException(capitalize(kindName) + " cannot connect a location to itself: " + upperCode);

        OrderedEndpoints ends = orderEndpoints(c1, c2, loc1, loc2);
        return new ParsedEndpoints(upperCode, ends.from(), ends.to());
    }

    private void register(String rawInputCode, String normalizedCode, T value) {
        if (byCode.containsKey(normalizedCode))
            throw new IllegalStateException("Duplicate " + kindName + " code: " + rawInputCode);
        byCode.put(normalizedCode, value);
        all.add(value);
    }

    private static String capitalize(String phrase) {
        return Character.toUpperCase(phrase.charAt(0)) + phrase.substring(1);
    }

    /**
     * Resolves a code (any case) to its value, failing if unknown.
     */
    public final T get(String code) {
        T result = find(code);
        if (result == null)
            throw new IllegalArgumentException("Unknown " + kindName + " code: " + code);
        return result;
    }

    public final boolean exists(String code) {
        return find(code) != null;
    }

    /**
     * @return an unmodifiable view of all registered values, in insertion order.
     */
    public final List<T> all() {
        return Collections.unmodifiableList(all);
    }

    private T find(String code) {
        return (code == null || code.isBlank()) ? null : byCode.get(code.trim().toUpperCase());
    }

    /**
     * Creates one registry value from the upper-cased input code and ordered endpoints.
     */
    @FunctionalInterface
    protected interface ItemFactory<T> {
        T create(String upperCode, Location from, Location to);
    }

    protected record OrderedEndpoints(Location from, Location to) {}

    private record ParsedEndpoints(String code, Location from, Location to) {}
}
