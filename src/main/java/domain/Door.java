package domain;

import domain.registry.DoorRegistry;

import java.util.Objects;

/**
 * Immutable value object for a single door (passable opening): occupants / hose crews pass from {@link #getFrom()}
 * toward {@link #getTo()}. A door that leaves the modeled deck entirely (an escape ladder / trunk, not a passage to
 * another mapped compartment) has {@link #getTo()} equal to the {@link Location#OUT} sentinel rather than a real
 * compartment.
 * <p>
 * Unlike a {@link Border}, a door does not delegate to a {@link Link}: it is not a subset of the bulkhead / link graph
 * the way a border is (a border always coincides with a registered wall). A door to {@link Location#OUT} has no wall
 * and no second mapped compartment, so it cannot be a {@link Link} at all — carrying the {@code (from, to)} pair
 * directly is exactly what lets this type represent that case. Interior doors are already handled as {@link Link} s
 * elsewhere (the runtime sealing path); {@code Door} exists specifically for the topology-initialization emission that
 * must also cover the off-deck exits.
 * <p>
 * Equality and hash code are value-based on {@code (from, to)}. Instances are normally built by {@link DoorRegistry};
 * construct directly only in tests or other code that already has validated endpoints in hand.
 * <p>
 * Kept as a class (not a {@code record}) for a stable {@code getXxx()} API consistent with the other
 * topology-identity value objects. Suppresses {@code java:S6206}.
 */
@SuppressWarnings("java:S6206")
public final class Door {
    private final Location from;
    private final Location to;

    public Door(Location from, Location to) {
        this.from = from;
        this.to = to;
    }

    public Location getFrom() {
        return from;
    }

    /** {@link Location#OUT} means this door leads off the modeled deck entirely. */
    public Location getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Door door)) return false;
        return Objects.equals(from, door.from) && Objects.equals(to, door.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + "->" + to;
    }
}
