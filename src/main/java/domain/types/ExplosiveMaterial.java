package domain.types;

import domain.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * The explosive-risk material present in a {@link Location}, driving which {@code IMMEDIATE-EXPLOSION} rule (and
 * recommended action) applies in {@code feis.clp}: {@link #DIESEL_OIL} → pump out, {@link #COMPRESSED_AIR} → carry out,
 * {@link #CHEMICAL_REAGENT} → fight directly. A location has at most one, matching the single (non-multi) CLIPS slot —
 * {@link Location#getExplosiveMaterial()} returns it as an {@code Optional}.
 * <p>
 * This is compartment identity, not a user-action value — sourced once from {@code topology.yaml} and seeded into CLIPS
 * as-is — so it lives in the domain rather than {@code clips.values.internal}. YAML uses {@link #fromName(String)}
 * (constant name lower-cased); CLIPS uses {@link #getClipsValue()}/{@link #fromClipsValue(String)} (same tokens for
 * this enum today). No Jackson annotations on purpose. Distinct from {@link ExplosiveType} (GUI button choice).
 */
public enum ExplosiveMaterial {
    CHEMICAL_REAGENT("chemical_reagent"),
    DIESEL_OIL("diesel_oil"),
    COMPRESSED_AIR("compressed_air");

    private static final Map<String, ExplosiveMaterial> NAME_TO_VALUE = new HashMap<>();
    private static final Map<String, ExplosiveMaterial> CLIPS_CODE_TO_VALUE = new HashMap<>();
    static {
        for (ExplosiveMaterial value : ExplosiveMaterial.values()) {
            NAME_TO_VALUE.put(value.name().toLowerCase(), value);
            NAME_TO_VALUE.put(value.name().toUpperCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.getClipsValue().toLowerCase(), value);
            CLIPS_CODE_TO_VALUE.put(value.getClipsValue().toUpperCase(), value);
        }
    }

    private final String clipsValue;

    ExplosiveMaterial(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }

    public static ExplosiveMaterial fromName(String name) {
        ExplosiveMaterial material = NAME_TO_VALUE.get(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown ExplosiveMaterial value: " + name);
        } else {
            return material;
        }
    }

    public static ExplosiveMaterial fromClipsValue(String clipsValue) {
        ExplosiveMaterial material = CLIPS_CODE_TO_VALUE.get(clipsValue);
        if (material == null) {
            throw new IllegalArgumentException("Unknown explosive CLIPS material: " + clipsValue);
        } else {
            return material;
        }
    }
}
