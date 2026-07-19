package domain;

import domain.registry.ExtinguisherRegistry;
import domain.types.ExtinguisherType;

import java.util.Objects;

/**
 * Immutable value object for a portable fire extinguisher (e.g. {@code "est_a "}). The title is normalized to lower
 * case ({@code trim().toLowerCase()}). Equality and hash code are value-based (on the title only —
 * {@code location}/{@code type} are metadata, not identity). Instances are normally built by
 * {@link ExtinguisherRegistry}, which resolves {@code location} from the title; construct directly only in tests or
 * other code that already has validated data in hand.
 * <p>
 * Not a {@code record}: equality is on {@code title} only ({@code location}/{@code type} are
 * metadata), so the generated all-component contract would be wrong. Suppresses {@code java:S6206}.
 */
@SuppressWarnings("java:S6206")
public final class Extinguisher {
    private final String title;
    private final Location location;
    private final ExtinguisherType type;

    public Extinguisher(String title, Location location, ExtinguisherType type) {
        this.title = title;
        this.location = location;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public Location getLocation() {
        return location;
    }

    public ExtinguisherType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Extinguisher that = (Extinguisher) o;
        return title.equals(that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

    @Override
    public String toString() {
        return title;
    }
}
