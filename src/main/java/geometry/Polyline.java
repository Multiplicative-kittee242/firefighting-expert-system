package geometry;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Open path as an ordered list of {@link Point} s (e.g. a bulkhead outline in {@code geometry.yaml}). Assembled on the
 * GUI side from config specs; not part of {@code domain}.
 */
public record Polyline(
    @JsonProperty("points") List<Point> points
) {}

