package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw {@code fire-hose-spans} section of {@code topology.yaml}: the two hose-reach graphs sharing the
 * {@link FireHoseSpanSpec} shape, kept in explicit separate lists (rather than one flat list with type-sniffing) since
 * {@code from} means a different thing in each — a door code in {@link #doorToDoor()}, a hydrant title in
 * {@link #hydrantToDoor()}.
 */
public record FireHoseSpansSpec(
    @JsonProperty("door-to-door")    List<FireHoseSpanSpec> doorToDoor,
    @JsonProperty("hydrant-to-door") List<FireHoseSpanSpec> hydrantToDoor
) {}
