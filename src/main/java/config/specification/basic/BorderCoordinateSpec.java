package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;
import geometry.Point;

import java.util.List;

/**
 * Geometry for one border (bulkhead) in geometry.yaml. Explicitly associates a link code with its outline polyline.
 * Replaces the previous positional indexing in border-coordinates.
 */
public record BorderCoordinateSpec(
    @JsonProperty("link")   String link,
    @JsonProperty("points") List<Point> points
) {}
