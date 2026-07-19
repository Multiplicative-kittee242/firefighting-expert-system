package config.specification.buttons;

import com.fasterxml.jackson.annotation.JsonProperty;
import geometry.Point;

/**
 * The fire sensor button's glyph type is not authored here: it is derived from {@code domain.FireSensor#getType()} at
 * button-group construction time (see {@code gui.map.input.FireSensorButtonGroup}), since it is the same value
 * {@code topology.yaml}'s {@code fire-sensor-codes} already carries — authoring it a second time here risked silent
 * drift between the two.
 */
public record FireSensorButtonSpec(
    @JsonProperty("sensorCode") String sensorCode,
    @JsonProperty("position")   Point position
) {}
