package domain.types;

import domain.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * The combustible material present in a {@link Location}, driving which {@code IMMEDIATE-ISOLATION} rule (and
 * recommended action) applies in {@code feis.clp}: {@link #MACHINE_OIL} → pump out, {@link #WORKING_CLOTHES} → carry
 * out. A location has at most one, matching the single (non-multi) CLIPS slot — {@link Location#getBurningMaterial()}
 * returns it as an {@code Optional}.
 * <p>
 * This is compartment identity, not a user-action value — sourced once from {@code topology.yaml} and seeded into CLIPS
 * as-is — so it lives in the domain rather than {@code clips.values.internal}. YAML uses {@link #fromName(String)}
 * (constant name lower-cased); CLIPS uses {@link #getClipsValue()}/{@link #fromClipsValue(String)} (same tokens for
 * this enum today). No Jackson annotations on purpose. Distinct from {@link PreventionType} (GUI button choice).
 */
public enum FlammableMaterial {
    MACHINE_OIL("machine_oil"),
    WORKING_CLOTHES("working_clothes");

    private static final Map<String, FlammableMaterial> NAME_TO_VALUE = new HashMap<>();
    private static final Map<String, FlammableMaterial> CLIPS_CODE_TO_VALUE = new HashMap<>();
    static {
        for (FlammableMaterial value : FlammableMaterial.values()) {
            NAME_TO_VALUE.put(value.name().toLowerCase(), value);
            NAME_TO_VALUE.put(value.name().toUpperCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.getClipsValue().toLowerCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.getClipsValue().toUpperCase(), value);
        }
    }

    private final String clipsValue;

    FlammableMaterial(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }

    public static FlammableMaterial fromName(String name) {
        FlammableMaterial material = NAME_TO_VALUE.get(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown FlammableMaterial value: " + name);
        } else {
            return material;
        }
    }

    public static FlammableMaterial fromClipsValue(String clipsValue) {
        FlammableMaterial material = CLIPS_CODE_TO_VALUE.get(clipsValue);
        if (material == null) {
            throw new IllegalArgumentException("Unknown burning CLIPS material: " + clipsValue);
        } else {
            return material;
        }
    }
}
