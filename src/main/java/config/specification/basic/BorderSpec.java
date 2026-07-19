package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw topology description of a bulkhead between two compartments: the 2-character {@code link} code (e.g.
 * {@code "DE "}) and the shared-wall {@code length} in metres. Fire spreads across borders (both directions); the
 * length feeds the CLIPS fire-line perimeter / hydrant-distance calculations.
 */
public record BorderSpec(
    @JsonProperty("link")   String link,
    @JsonProperty("length") double length
) {}
