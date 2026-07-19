package domain.registry;

import domain.types.FireSensorType;
import domain.FireSensor;
import domain.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only in-memory registry of {@link FireSensor} value objects, built once from a map of raw sensor code to
 * {@link FireSensorType}. Depends on a {@link LocationRegistry} to resolve each sensor's owning {@link Location} (its
 * first character). Codes are normalized to upper case; lookup is case-insensitive; duplicate codes fail fast.
 */
public final class FireSensorRegistry {
    private final Map<String, FireSensor> byCode = new LinkedHashMap<>();
    private final List<FireSensor> all = new ArrayList<>();

    public FireSensorRegistry(Map<String, FireSensorType> sensorTypes, LocationRegistry locations) {
        for (Map.Entry<String, FireSensorType> entry : sensorTypes.entrySet()) {
            String raw = entry.getKey();
            if (raw != null && !raw.isBlank()) {
                FireSensor sensor = createFireSensor(raw, entry.getValue(), locations);
                String key = sensor.getCode();
                if (byCode.putIfAbsent(key, sensor) != null)
                    throw new IllegalStateException("Duplicate fire sensor code: " + raw);
                all.add(sensor);
            }
        }
    }

    private static FireSensor createFireSensor(String rawCode, FireSensorType type, LocationRegistry locations) {
        String code = rawCode.trim().toUpperCase();
        Location location = locations.get(code.substring(0, 1));
        return new FireSensor(code, location, type);
    }

    /**
     * Resolves a code (any case) to its {@link FireSensor}, failing if unknown.
     */
    public FireSensor get(String code) {
        FireSensor sensor = find(code);
        if (sensor == null)
            throw new IllegalArgumentException("Unknown fire sensor code: " + code);
        return sensor;
    }

    public boolean exists(String code) {
        return find(code) != null;
    }

    /**
     * @return an unmodifiable view of all registered sensors, in insertion order.
     */
    public List<FireSensor> all() {
        return Collections.unmodifiableList(all);
    }

    private FireSensor find(String code) {
        return (code == null || code.isBlank()) ? null : byCode.get(code.trim().toUpperCase());
    }
}
