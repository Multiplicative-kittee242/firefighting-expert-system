package domain;

import domain.registry.BorderRegistry;

import java.util.Objects;

/**
 * Immutable value object for a single bulkhead (shared wall) between two compartments, carrying its shared-wall length
 * in metres. Fire spreads across borders in both directions (unlike a {@link Door}, which is a passable opening a
 * person or hose crew must go through) — the length feeds the CLIPS fire-line perimeter / hydrant-count calculation
 * (see {@code clips.ClipsEngineAccess#getFireLineHydrantsNeeded}).
 * <p>
 * Wraps a plain {@link Link} for identity — a border, like a {@code Link}, is undirected — plus the length {@code Link}
 * itself does not carry. Equality and hash code are value-based on the link. Instances are normally built by
 * {@link BorderRegistry}; construct directly only in tests or other code that already has a validated {@link Link} in
 * hand.
 * <p>
 * Not a {@code record}: equality is on {@code link} only ({@code length} is metadata), so the
 * generated all-component contract would be wrong. Suppresses {@code java:S6206}.
 */
@SuppressWarnings("java:S6206")
public final class Border {
    private final Link link;
    private final double length;

    public Border(Link link, double length) {
        this.link = link;
        this.length = length;
    }

    public Link getLink() {
        return link;
    }

    /** The shared-wall length in metres. */
    public double getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Border border)) return false;
        return link.equals(border.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link);
    }

    @Override
    public String toString() {
        return link + " (" + length + "m)";
    }
}
