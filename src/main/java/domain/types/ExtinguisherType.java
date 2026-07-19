package domain.types;

import domain.Extinguisher;

import java.util.HashMap;
import java.util.Map;

/**
 * The extinguishing agent of a portable {@link Extinguisher}. This is device identity, not a user-action value —
 * sourced once from {@code topology.yaml} and seeded into CLIPS as-is — so it lives in the domain rather than
 * {@code clips.values.internal}: CLIPS reads it only to pick the wording of one printout line (
 * {@code IMMEDIATE-EXTINGUISHERS::use-local} in {@code feis.clp}), it never gates rule activation or is written back
 * from a user action.
 * <p>
 * Two string spaces: YAML authors {@link #fromName(String)} tokens ({@code carbon_dioxide}/{@code air_foam}); CLIPS is
 * seeded with {@link #getClipsValue()} ({@code co}/{@code af}). No Jackson annotations on purpose.
 */
public enum ExtinguisherType {
    CARBON_DIOXIDE("co"),
    AIR_FOAM("af");

    private static final Map<String, ExtinguisherType> NAME_TO_VALUE = new HashMap<>();
    private static final Map<String, ExtinguisherType> CLIPS_CODE_TO_VALUE = new HashMap<>();
    static {
        for (ExtinguisherType value : ExtinguisherType.values()) {
            NAME_TO_VALUE.put(value.name().toLowerCase(), value);
            NAME_TO_VALUE.put(value.name().toUpperCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.getClipsValue().toLowerCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.getClipsValue().toUpperCase(), value);
        }
    }

    private final String clipsValue;

    ExtinguisherType(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }

    public static ExtinguisherType fromName(String name) {
        ExtinguisherType type = NAME_TO_VALUE.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown ExtinguisherType value: " + name);
        } else {
            return type;
        }
    }

    public static ExtinguisherType fromClipsValue(String clipsValue) {
        ExtinguisherType type = CLIPS_CODE_TO_VALUE.get(clipsValue);
        if (type == null) {
            throw new IllegalArgumentException("Unknown extinguisher CLIPS type: " + clipsValue);
        } else {
            return type;
        }
    }
}
