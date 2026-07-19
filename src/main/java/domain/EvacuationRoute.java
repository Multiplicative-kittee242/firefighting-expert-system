package domain;

import domain.registry.EvacuationRouteRegistry;

import java.util.Objects;

/**
 * Immutable value object for a single directed evacuation edge: occupants of {@link #getFrom()} escape <em>toward</em>
 * {@link #getTo()}. Direction is primary data, not an alphabetical convenience — the underlying graph declares some
 * passages in both directions as two separate routes (e.g. {@code e→f} and {@code f→e}), so unlike an undirected
 * {@link Link} the endpoints may not be swapped.
 * <p>
 * The {@code code} is the two endpoint codes upper-cased in {@code from} -then- {@code to} order (e.g. {@code "EF "}
 * for {@code e→f}), which is therefore directional: {@code e→f} and {@code f→e} have distinct codes. Equality and hash
 * code are value-based on that code. Instances are normally built by {@link EvacuationRouteRegistry}, which resolves
 * the endpoints against the location registry; construct directly only in tests or code that already has validated
 * endpoints in hand.
 */
public final class EvacuationRoute {
    private final String code;
    private final Location from;
    private final Location to;

    public EvacuationRoute(Location from, Location to) {
        this.from = from;
        this.to = to;
        this.code = from.getCode().toUpperCase() + to.getCode().toUpperCase();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvacuationRoute route)) return false;
        return code.equals(route.code);
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
