package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw topology description of one directed evacuation edge: occupants of {@code from} escape <em>toward</em>
 * {@code to}. Both are location codes; unlike a bulkhead the order is significant (it is the escape direction), and the
 * same passage may be declared in both directions as two separate routes. Resolved into a
 * {@code domain.EvacuationRoute} by {@code domain.registry.EvacuationRouteRegistry}.
 */
public record EvacuationRouteSpec(
    @JsonProperty("from") String from,
    @JsonProperty("to")   String to
) {}
