package geometry;

/**
 * Integer pixel coordinate on the deck map. Shared by {@code config} (YAML geometry / placements) and {@code gui}
 * (layout); deliberately not used by {@code domain}, which is coordinate-free.
 */
public record Point(int x, int y) {}

