package config.specification.buttons;

import com.fasterxml.jackson.annotation.JsonProperty;
import config.specification.LocationAttached;
import geometry.Point;

/**
 * The explosion-prevention button's glyph type is not authored here: it is derived from
 * {@code domain.Location#getExplosiveMaterial()} at button-group construction time (see
 * {@code gui.map.input.ExplosionButtonGroup}), since it is the same value {@code topology.yaml}'s
 * {@code location-labels} already carries — authoring it a second time here risked silent drift between the two.
 */
public record ExplosionButtonSpec(
    @JsonProperty("locationCode") String locationCode,
    @JsonProperty("position")     Point position
) implements LocationAttached {}
