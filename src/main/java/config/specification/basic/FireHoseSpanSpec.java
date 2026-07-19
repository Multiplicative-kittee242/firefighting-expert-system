package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw topology description of one fire-hose reach segment: the distance (metres) a hose must run from {@code from} to
 * the door {@code to}. Used for two distinct graphs sharing this same shape — see {@code topology.yaml}'s
 * {@code fire-hose-spans.door-to-door} (both endpoints are door codes, e.g. {@code "DE "}/{@code "DQ "}) and
 * {@code fire-hose-spans.hydrant-to-door} ({@code from} is a hydrant title, e.g. {@code "hydr_d1"}, {@code to} is a
 * door code). Resolved into a {@code domain.FireHoseSpan} by {@code domain.registry.FireHoseSpanRegistry}.
 */
public record FireHoseSpanSpec(
    @JsonProperty("from")     String from,
    @JsonProperty("to")       String to,
    @JsonProperty("distance") double distance
) {}
