package domain.registry;

import domain.types.*;
import domain.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only in-memory registry (DDD lookup service) of {@link Location} value objects, built once from a list of raw
 * per-location specs ({@link RawLocation}). Replaces the former global mutable static state on {@link Location}: it is
 * instantiated in the composition root and injected wherever a code needs to be resolved. Lookup is case-insensitive;
 * duplicate codes fail fast.
 */
public final class LocationRegistry {
    private final Map<String, Location> byCode = new LinkedHashMap<>();
    private final List<Location> all = new ArrayList<>();

    public LocationRegistry(List<RawLocation> rawLocations) {
        for (RawLocation raw : rawLocations) {
            if (raw != null && raw.code() != null && !raw.code().isBlank()) {
                Location location = new Location(raw.code(), raw.area(), raw.tank(), raw.type(), raw.ventilationType(),
                    raw.explosiveMaterial(), raw.flammableMaterial(), raw.hasMachinery(), raw.hasChemicalSuppression());
                String key = location.getCode();
                if (byCode.putIfAbsent(key, location) != null)
                    throw new IllegalStateException("Duplicate location code: " + raw.code());
                all.add(location);
            }
        }
    }

    /**
     * Resolves a code (any case) to its {@link Location}, failing if unknown.
     */
    public Location get(String code) {
        Location location = find(code);
        if (location == null)
            throw new IllegalArgumentException("Unknown location code: " + code);
        return location;
    }

    public boolean exists(String code) {
        return find(code) != null;
    }

    /**
     * @return an unmodifiable view of all registered locations, in insertion order.
     */
    public List<Location> all() {
        return Collections.unmodifiableList(all);
    }

    private Location find(String code) {
        return (code == null || code.isBlank()) ? null : byCode.get(code.trim().toLowerCase());
    }

    /**
     * A raw per-location description prior to identity / attribute resolution. Kept independent of any config / Jackson
     * type so this registry has no dependency on the {@code config} package (see {@link FireHoseSpanRegistry.RawSpan}
     * for the same convention). {@link #identity(String)} builds an attributes-at-default spec for callers that only
     * need a bare code (tests, ad-hoc identity references).
     */
    public record RawLocation(String code, double area, int tank, CompartmentType type, VentilationType ventilationType,
                              ExplosiveMaterial explosiveMaterial, FlammableMaterial flammableMaterial, boolean hasMachinery,
                              boolean hasChemicalSuppression)
    {
        public static RawLocation identity(String code) {
            return new RawLocation(code, Location.NO_AREA, Location.DEFAULT_TANK, CompartmentType.UNINHABITED,
                null, null, null, false, false);
        }
    }
}
