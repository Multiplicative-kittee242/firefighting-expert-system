package domain.types;

import domain.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * Structural classification of a {@link Location}'s compartment role, used by the hose-routing path-cost weighting in
 * the CLIPS rule base ({@code EXTINGUISHING-GRAPH::decompose-arrays} in {@code feis.clp}: engine rooms weigh most, then
 * posts, then service / auxiliary spaces). {@link #UNINHABITED} is the Java-only default for the compartments
 * {@code feis.clp} never assigned a type to (plain passageways and the R / T tank compartments) —
 * {@code (default none)} on the CLIPS side.
 * <p>
 * This is compartment identity, not a user-action value — sourced once from {@code topology.yaml} and seeded into CLIPS
 * as-is — so it lives in the domain rather than {@code clips.values.internal}. Two string spaces: YAML uses
 * {@link #fromName(String)} (constant name lower-cased, e.g. {@code engine_room}, {@code auxiliary}); CLIPS uses
 * {@link #getClipsValue()}/{@link #fromClipsValue(String)} ({@code engine-room}, {@code auxilary} — historical spelling
 * in {@code feis.clp}). No Jackson annotations on purpose.
 */
public enum CompartmentType {
    POST("post"),
    SERVICE("service"),
    AUXILIARY("auxilary"),
    ENGINE_ROOM("engine-room"),
    UNINHABITED("none");

    private static final Map<String, CompartmentType> NAME_TO_VALUE = new HashMap<>();
    private static final Map<String, CompartmentType> CLIPS_CODE_TO_VALUE = new HashMap<>();
    static {
        for (CompartmentType value : CompartmentType.values()) {
            NAME_TO_VALUE.put(value.name().toLowerCase(), value);
            NAME_TO_VALUE.put(value.name().toUpperCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.getClipsValue().toLowerCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.getClipsValue().toUpperCase(), value);
        }
    }

    private final String clipsValue;

    CompartmentType(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }

    public static CompartmentType fromName(String name) {
        CompartmentType type = NAME_TO_VALUE.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown CompartmentType value: " + name);
        } else {
            return type;
        }
    }

    public static CompartmentType fromClipsValue(String clipsValue) {
        CompartmentType type = CLIPS_CODE_TO_VALUE.get(clipsValue);
        if (type == null) {
            throw new IllegalArgumentException("Unknown compartment CLIPS type: " + clipsValue);
        } else {
            return type;
        }
    }
}
