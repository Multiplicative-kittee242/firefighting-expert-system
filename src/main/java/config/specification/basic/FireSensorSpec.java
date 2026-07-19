package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw topology description of one fire sensor sub-code (e.g. {@code "A1"}) and its physical sensor type. {@code type}
 * is the YAML name vocabulary — {@code domain.types.FireSensorType}'s constant name lower-cased (
 * {@code temperature}/{@code combined}/{@code rate_of_rise}), resolved by {@code DeckMapTopologyConfig} via
 * {@code FireSensorType#fromName(String)}. CLIPS has no concept of sensors (an accident is reported on the owning
 * location), so there is no CLIPS wire token to author here. Kept a plain string here (not the domain enum itself) so
 * this record has no dependency beyond Jackson annotations, matching every other raw spec in this package.
 */
public record FireSensorSpec(
    @JsonProperty("code") String code,
    @JsonProperty("type") String type
) {}
