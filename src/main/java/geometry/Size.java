package geometry;

/**
 * Integer width / height in pixels for map controls and glyphs. Shared by {@code config} and {@code gui}; not a domain
 * concept.
 */
public record Size(int width, int height) {}

