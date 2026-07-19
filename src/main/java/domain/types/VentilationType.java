package domain.types;

import domain.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * The ventilation system installed in a {@link Location}: topological identity — which physical
 * ventilation system serves the compartment — not a Swing glyph choice; CLIPS itself never reads it,
 * only the on / off status derived from its presence, see {@code clips.ClipsEngineAccess#fromLocationInstance}. A
 * location has at most one, matching {@link Location#getVentilationType()}'s {@code Optional} return.
 * <p>
 * {@link #TRANSIT} is a duct penetrating a fire-rated bulkhead (fitted with a fire damper), {@link #SMOKE_CONTROL} a
 * dedicated smoke-exhaust system (RWA), {@link #BASIC} ordinary / general ventilation. Each locale's glyph and full
 * name live in {@code i18n/messages*.properties} under {@code label.ventilation.<name>.short}/{@code .full}, keyed off
 * {@link #name()} — not on this enum, to keep {@code domain} free of any presentation-framework dependency.
 */
public enum VentilationType {
    BASIC,
    SMOKE_CONTROL,
    TRANSIT;

    private static final Map<String, VentilationType> NAME_TO_VALUE = new HashMap<>();
    private static final Map<String, VentilationType> CLIPS_CODE_TO_VALUE = new HashMap<>();
    static {
        for (VentilationType value : VentilationType.values()) {
            NAME_TO_VALUE.put(value.name().toLowerCase(), value);
            NAME_TO_VALUE.put(value.name().toUpperCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.name().toLowerCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.name().toUpperCase(), value);
        }
    }

    public static VentilationType fromName(String name) {
        VentilationType type = NAME_TO_VALUE.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown VentilationType value: " + name);
        } else {
            return type;
        }
    }

    public static VentilationType fromClipsValue(String clipsValue) {
        VentilationType type = CLIPS_CODE_TO_VALUE.get(clipsValue);
        if (type == null) {
            throw new IllegalArgumentException("Unknown ventilation CLIPS type: " + clipsValue);
        } else {
            return type;
        }
    }
}
