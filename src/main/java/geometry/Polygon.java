package geometry;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Closed outline as an ordered list of {@link Point} s (e.g. a compartment polygon in {@code geometry.yaml}). Assembled
 * on the GUI side from config specs; not part of {@code domain}.
 */
public record Polygon(
    @JsonProperty("points") List<Point> points
) {}

