package domain;

import java.util.Objects;

/**
 * Immutable value object for a single hose-reach distance segment used by the fire-line hydrant search: how far (in
 * metres) a fire hose must run to get from {@link #getFrom()} to the door {@link #getTo()}.
 * <p>
 * {@code F} is the origin's type — either a {@link Link} (a door-to-door segment, the distance between two adjacent
 * door centres) or a {@link HydrantOutlets} (a hydrant-to-door segment, the distance from a hydrant's outlet to the
 * nearest door). {@code to} is always a {@link Link}: a door is the addressable unit on both sides, so a span always
 * ends at one.
 * <p>
 * Equality and hash code are value-based on {@code (from, to)}.
 * <p>
 * Not a {@code record}: equality is on {@code (from, to)} only ({@code distance} is metadata), so
 * the generated all-component contract would be wrong. Suppresses {@code java:S6206}.
 */
@SuppressWarnings("java:S6206")
public final class FireHoseSpan<F> {
    private final F from;
    private final Link to;
    private final double distance;

    public FireHoseSpan(F from, Link to, double distance) {
        this.from = from;
        this.to = to;
        this.distance = distance;
    }

    public F getFrom() {
        return from;
    }

    public Link getTo() {
        return to;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FireHoseSpan<?> that)) return false;
        return Objects.equals(from, that.from) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + "->" + to + " (" + distance + "m)";
    }
}
