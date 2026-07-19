package domain;

import domain.registry.LinkRegistry;

import java.util.Objects;

/**
 * Immutable value object for a border link (a door / wall) between two {@link Location} s. The {@code code} preserves
 * the (uppercased) two-character input order, while {@code from}/{@code to} are normalized so {@code from} is the
 * alphabetically-earlier endpoint. Equality and hash code are value-based (on the code). Instances are normally built
 * and validated by {@link LinkRegistry}; construct directly only in tests or other code that already has validated
 * endpoints in hand.
 * <p>
 * Not a {@code record}: equality is on {@code code} only ({@code from}/{@code to} are derived
 * endpoints, not identity), so the generated all-component contract would be wrong. Suppresses
 * {@code java:S6206}.
 */
@SuppressWarnings("java:S6206")
public final class Link {
    private final String code;
    private final Location from;
    private final Location to;

    public Link(String code, Location from, Location to) {
        this.code = code;
        this.from = from;
        this.to = to;
    }

    public String getCode() {
        return code;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public Location getOtherSide(Location side) {
        if (side.equals(from)) return to;
        if (side.equals(to)) return from;
        throw new IllegalArgumentException("Location " + side + " is not connected by this link");
    }

    public boolean connects(Location a, Location b) {
        return (a.equals(from) && b.equals(to)) || (a.equals(to) && b.equals(from));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Link link)) return false;
        return Objects.equals(code, link.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
