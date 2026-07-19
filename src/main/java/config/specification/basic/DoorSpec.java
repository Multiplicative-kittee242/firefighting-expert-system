package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;
import domain.Location;

/**
 * Raw topology description of a door (passable opening). {@code from} is always a location code; {@code to} is either
 * another location code or the external-deck sentinel ({@link #EXTERNAL_DECK}), which marks an exit that leaves the
 * modeled compartment for another deck. {@code fireRated} marks a fire-rated door.
 */
public record DoorSpec(
    @JsonProperty("from")      String from,
    @JsonProperty("to")        String to,
    @JsonProperty("fireRated") boolean fireRated
) {
    /**
     * The deck-exit token in {@code to} — the code of the {@link Location#OUT} sentinel (the canonical definition,
     * needed by both the {@code config} and {@code clips} layers), re-exported here as a plain {@link String} for
     * callers that already reference this raw config DTO.
     */
    public static final String EXTERNAL_DECK = Location.OUT.getCode();
}
