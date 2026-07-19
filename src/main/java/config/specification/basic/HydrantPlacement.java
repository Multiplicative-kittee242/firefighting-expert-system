package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;
import geometry.Point;

public record HydrantPlacement(
    @JsonProperty("code")     String code,
    @JsonProperty("position") Point position,
    @JsonProperty("count")    int count
) {}
