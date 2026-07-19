package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;
import geometry.Point;

import java.util.List;

/**
 * Geometry for one location (compartment) in geometry.yaml. Explicitly associates a location code with its outline
 * polygon. Replaces the previous positional indexing in location-coordinates.
 */
public record LocationCoordinateSpec(
    @JsonProperty("code")   String code,
    @JsonProperty("points") List<Point> points
) {}
