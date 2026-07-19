package config.specification.buttons;

import com.fasterxml.jackson.annotation.JsonProperty;
import config.specification.LocationAttached;
import geometry.Point;

/**
 * No glyph type here: every machinery-damage button is the same glyph ({@code domain.types.PreventionType#MECHANICAL}),
 * hardcoded in {@code gui.map.input.MachineryDamageButtonGroup} rather than repeated as a constant per item.
 */
public record MachineryDamageButtonSpec(
    @JsonProperty("locationCode") String locationCode,
    @JsonProperty("position")     Point position
) implements LocationAttached {}
