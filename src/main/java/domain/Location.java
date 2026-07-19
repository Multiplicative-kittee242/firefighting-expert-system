package domain;

import domain.registry.LocationRegistry;
import domain.types.FlammableMaterial;
import domain.types.CompartmentType;
import domain.types.ExplosiveMaterial;
import domain.types.VentilationType;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value object identifying a single room / compartment, enriched with the scenario attributes
 * {@code feis.clp} used to seed onto the {@code LOCATION} COOL instance (the former
 * {@code location-attrs}/{@code apply-location-attributes}, now sourced from {@code topology.yaml} instead — see
 * {@code clips.ClipsEngineAccess#fromLocationInstance}). Equality and hash code stay identity-based (on the normalized
 * {@link #getCode() code} only) — the attributes below are descriptive, not identity, exactly like
 * {@link Extinguisher#getType()}.
 * <p>
 * Codes are normalized to lower case ({@code trim().toLowerCase()}) on construction. Instances are normally obtained
 * through {@link LocationRegistry}, which validates raw codes and interns them; construct directly only in tests or
 * other code that already has validated data in hand — the single-argument constructor builds an identity-only location
 * with every attribute at its CLIPS-matching default ({@link #NO_AREA}, {@link #DEFAULT_TANK},
 * {@link CompartmentType#UNINHABITED}, no ventilation system / explosive / burning material / machinery / chemical
 * suppression).
 */
public final class Location {
    /**
     * Sentinel for "no {@link #getArea()} assigned" (CLIPS's own {@code no-area}) — no real compartment has zero area.
     */
    public static final double NO_AREA = 0.0;
    /** CLIPS's own {@code (slot tank (default 3))}. */
    public static final int DEFAULT_TANK = 3;
    /**
     * Sentinel destination for a door that leaves the modeled deck entirely — an escape ladder / trunk to another deck,
     * rather than a passage into another mapped compartment (see {@link Door#getTo()}). Its {@link #getCode() code} is
     * the literal {@code out} token that both {@code clips.ClipsEngineAccess} and the config / YAML layer use for such
     * a door's destination.
     * <p>
     * <b>Not a real compartment:</b> it is never registered in {@link LocationRegistry} and never appears in
     * {@link domain.registry.TopologyModel#allLocations()} or any rendering — it exists only as a {@code Door}'s
     * {@code to} endpoint. Treat it purely as an identity / code carrier; its other attributes are the identity-only
     * defaults and are never read.
     */
    public static final Location OUT = new Location("out");

    private final String code;
    private final double area;
    private final int tank;
    private final CompartmentType type;
    private final VentilationType ventilationType;
    private final ExplosiveMaterial explosiveMaterial;
    private final FlammableMaterial flammableMaterial;
    private final boolean hasMachinery;
    private final boolean hasChemicalSuppression;

    public Location(String rawCode) {
        this(rawCode, NO_AREA, DEFAULT_TANK, CompartmentType.UNINHABITED, null, null, null, false, false);
    }

    // Full attribute list mirrors CLIPS location slots / RawLocation; production builds via registry.
    @SuppressWarnings("java:S107")
    public Location(String rawCode, double area, int tank, CompartmentType type, VentilationType ventilationType,
        ExplosiveMaterial explosiveMaterial, FlammableMaterial flammableMaterial, boolean hasMachinery,
        boolean hasChemicalSuppression)
    {
        String normalized = rawCode == null ? "" : rawCode.trim().toLowerCase();
        if (normalized.isEmpty())
            throw new IllegalArgumentException("Location code cannot be blank");
        this.code = normalized;
        this.area = area;
        this.tank = tank;
        this.type = type;
        this.ventilationType = ventilationType;
        this.explosiveMaterial = explosiveMaterial;
        this.flammableMaterial = flammableMaterial;
        this.hasMachinery = hasMachinery;
        this.hasChemicalSuppression = hasChemicalSuppression;
    }

    public String getCode() {
        return code;
    }

    /**
     * The compartment's floor area in square metres, or {@link #NO_AREA} if none is assigned (R / T tank compartments).
     */
    public double getArea() {
        return area;
    }

    /**
     * CLIPS {@code tank} attribute — a numeric identifier, redundant with the R / T fire-door topology.
     */
    public int getTank() {
        return tank;
    }

    public CompartmentType getType() {
        return type;
    }

    public Optional<VentilationType> getVentilationType() {
        return Optional.ofNullable(ventilationType);
    }

    public Optional<ExplosiveMaterial> getExplosiveMaterial() {
        return Optional.ofNullable(explosiveMaterial);
    }

    public Optional<FlammableMaterial> getBurningMaterial() {
        return Optional.ofNullable(flammableMaterial);
    }

    public boolean hasMachinery() {
        return hasMachinery;
    }

    /**
     * Whether the compartment has a fixed ОХТ (volumetric chemical suppression) system — CLIPS's {@code co} attribute.
     */
    public boolean hasChemicalSuppression() {
        return hasChemicalSuppression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location location)) return false;
        return code.equals(location.code);
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
