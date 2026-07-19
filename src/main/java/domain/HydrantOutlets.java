package domain;

import domain.registry.HydrantOutletsRegistry;

import java.util.Objects;

/**
 * Immutable value object for a hydrant-outlet label (e.g. {@code "hydr_d1"}). The title is normalized to lower case (
 * {@code trim().toLowerCase()}). Equality and hash code are value-based (on the title only —
 * {@code location}/{@code outlets} are metadata, not identity). Instances are normally built by
 * {@link HydrantOutletsRegistry}, which resolves {@code location} from the title and validates {@code outlets};
 * construct directly only in tests or other code that already has validated data in hand.
 * <p>
 * Not a {@code record}: equality is on {@code title} only ({@code location}/{@code outlets} are
 * metadata), so the generated all-component contract would be wrong. Suppresses {@code java:S6206}.
 */
@SuppressWarnings("java:S6206")
public final class HydrantOutlets {
    private final String title;
    private final Location location;
    private final int outlets;

    public HydrantOutlets(String title, Location location, int outlets) {
        this.title = title;
        this.location = location;
        this.outlets = outlets;
    }

    public String getTitle() {
        return title;
    }

    public Location getLocation() {
        return location;
    }

    public int getOutlets() {
        return outlets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HydrantOutlets that = (HydrantOutlets) o;
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
