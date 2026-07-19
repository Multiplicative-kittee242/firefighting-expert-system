package domain.types;

import domain.FireSensor;
import domain.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * The physical fire-sensor type installed at a {@link FireSensor}'s point: topological identity —
 * which sensor equipment sits at that exact spot — not a Swing glyph choice; CLIPS never reads it,
 * since it has no concept of sensors at all — an accident is reported directly on the owning
 * {@link Location}.
 * <p>
 * {@link #TEMPERATURE} is a fixed / static-temperature (max) detector, {@link #RATE_OF_RISE} a differential
 * (rate-of-rise) detector, {@link #COMBINED} a combination of both — matching Russian / international heat-detector
 * classification (RD 25.953-90 / EN 54-5). Each locale's glyph and full name live in {@code i18n/messages*.properties}
 * under {@code label.sensor.<name>.short}/{@code .full}, keyed off {@link #name()} — not on this enum, to keep
 * {@code domain} free of any presentation-framework dependency.
 */
public enum FireSensorType {
    TEMPERATURE,
    COMBINED,
    RATE_OF_RISE;

    private static final Map<String, FireSensorType> NAME_TO_VALUE = new HashMap<>();
    private static final Map<String, FireSensorType> CLIPS_CODE_TO_VALUE = new HashMap<>();
    static {
        for (FireSensorType value : FireSensorType.values()) {
            NAME_TO_VALUE.put(value.name().toLowerCase(), value);
            NAME_TO_VALUE.put(value.name().toUpperCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.name().toLowerCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.name().toUpperCase(), value);
        }
    }

    public static FireSensorType fromName(String name) {
        FireSensorType type = NAME_TO_VALUE.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown FireSensorType value: " + name);
        } else {
            return type;
        }
    }

    public static FireSensorType fromClipsValue(String clipsValue) {
        FireSensorType type = CLIPS_CODE_TO_VALUE.get(clipsValue);
        if (type == null) {
            throw new IllegalArgumentException("Unknown fire sensor CLIPS type: " + clipsValue);
        } else {
            return type;
        }
    }
}
