package config.specification;

import com.fasterxml.jackson.annotation.JsonProperty;
import config.enums.HydrantLabelSize;
import geometry.Point;

public record FirefightingStepGroupSpec(
    @JsonProperty("locationCode") String locationCode,
    @JsonProperty("position")     Point position,
    @JsonProperty("size") HydrantLabelSize size
) implements LocationAttached {}
