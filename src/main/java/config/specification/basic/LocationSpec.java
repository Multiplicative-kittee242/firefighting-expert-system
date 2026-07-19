package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw topology description of one compartment: its code plus the scenario attributes {@code feis.clp} used to hardcode
 * in {@code location-attrs} (area, tank, compartment type, ventilation system, explosive / burning material, machinery,
 * chemical suppression system). Every field but {@code code} is nullable / omittable, matching CLIPS's own
 * per-attribute defaults — omitted means "not present" ({@code type} defaults to {@code UNINHABITED},
 * {@code explosive}/{@code burning}/{@code ventilation} to absent, booleans to {@code false}, {@code area}/{@code tank}
 * to {@code domain.Location.NO_AREA}/{@code DEFAULT_TANK}).
 * <p>
 * Enum-like string fields are the <em>YAML name vocabulary</em> — each enum constant's name, lower-cased (e.g.
 * {@code engine_room}, {@code chemical_reagent}, {@code smoke_control}) — resolved by {@code DeckMapTopologyConfig} via
 * {@code fromName(...)}, not via {@code fromClipsValue}. That is a separate string space:
 * {@code getClipsValue()}/{@code fromClipsValue} round-trip {@code feis.clp}'s own tokens ({@code engine-room},
 * {@code auxilary}, …) only when seeding or reading the engine. {@code ventilation} is likewise a name token (
 * {@code basic}/{@code transit}/{@code smoke_control}); CLIPS never receives the ventilation <em>type</em>, only on /
 * off derived from its presence. Kept plain strings here (not the domain enums themselves) so this record has no
 * dependency beyond Jackson annotations, matching every other raw spec in this package.
 */
public record LocationSpec(
    @JsonProperty("code")                 String code,
    @JsonProperty("area")                 Double area,
    @JsonProperty("tank")                 Integer tank,
    @JsonProperty("type")                 String type,
    @JsonProperty("ventilation")          String ventilation,
    @JsonProperty("explosive")            String explosive,
    @JsonProperty("burning")              String burning,
    @JsonProperty("machinery")            Boolean machinery,
    @JsonProperty("chemical-suppression") Boolean chemicalSuppression
) {
    /** Convenience for tests/call sites that only need a bare code, with every attribute omitted. */
    public static LocationSpec identity(String code) {
        return new LocationSpec(code, null, null, null, null, null, null, null, null);
    }
}
