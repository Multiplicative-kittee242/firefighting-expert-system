package domain;

import domain.registry.FireSensorRegistry;
import domain.types.FireSensorType;

import java.util.Objects;

/**
 * Immutable value object for a fire sensor sub-code (e.g. {@code "A1"}). The code is normalized to upper case; the
 * owning {@link Location} is the first character. Equality and hash code are value-based (on the code) — {@code type}
 * is metadata, not identity, exactly like {@link Extinguisher#getType()}. Instances are normally built by
 * {@link FireSensorRegistry}, which resolves the {@link Location}; construct directly only in tests or other code that
 * already has validated data in hand.
 * <p>
 * Not a {@code record}: equality is on {@code code} only ({@code location}/{@code type} are
 * metadata), so the generated all-component contract would be wrong. Suppresses {@code java:S6206}.
 */
@SuppressWarnings("java:S6206")
public final class FireSensor {
    private final String code;
    private final Location location;
    private final FireSensorType type;

    public FireSensor(String code, Location location, FireSensorType type) {
        this.code = code;
        this.location = location;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public Location getLocation() {
        return location;
    }

    public FireSensorType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FireSensor sensor)) return false;
        return code.equals(sensor.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
