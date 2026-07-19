package config.specification;

import com.fasterxml.jackson.annotation.JsonProperty;
import domain.FireSensor;
import domain.HydrantOutlets;
import domain.Location;
import geometry.Point;

/**
 * Holds the placement ({@link Point}) of a single on-map element together with its domain key: the identifier that
 * selects it.
 * <p>
 * {@code D} is the domain key type — a registered {@link Location} for elements that map to a single room /
 * compartment, a {@link HydrantOutlets} for hydrant-outlet labels, a {@link FireSensor} for fire sensor sub-codes (all
 * via {@link #of}), or a {@link String} for everything else (hydrant labels, door pair codes) via {@link #raw}. Future
 * steps may introduce further domain key types (e.g. a dedicated door-pair type) without changing this class's shape.
 */
public record ElementPlacement<D>(
    @JsonProperty("code")       D key,
    @JsonProperty("position")   Point point)
{
    /**
     * Creates an element keyed by a registered {@link Location} (a single room / compartment code). Reserved for
     * element lists confirmed to contain only single-letter location codes that resolve via
     * {@code domain.registry.LocationRegistry}.
     */
    public static ElementPlacement<Location> of(Location location, Point point) {
        return new ElementPlacement<>(location, point);
    }

    /**
     * Creates an element keyed by a {@link HydrantOutlets} (a hydrant-outlet label, e.g. {@code "hydr_d1"}). Reserved
     * for element lists confirmed to contain only registered hydrant titles that resolve via
     * {@code domain.registry.HydrantOutletsRegistry}.
     */
    public static ElementPlacement<HydrantOutlets> of(HydrantOutlets hydrant, Point point) {
        return new ElementPlacement<>(hydrant, point);
    }

    /**
     * Creates an element keyed by a {@link FireSensor} (a fire sensor sub-code, e.g. {@code "A1"}). Reserved for
     * element lists confirmed to contain only registered sensor codes that resolve via
     * {@code domain.registry.FireSensorRegistry}.
     */
    public static ElementPlacement<FireSensor> of(FireSensor sensor, Point point) {
        return new ElementPlacement<>(sensor, point);
    }

    /**
     * Creates an element keyed by an arbitrary raw string: sensor sub-codes ({@code "A1"}), hydrant labels (
     * {@code "hydr_d1"}), door pair codes ({@code "AQ "}), and any other identifier that is not a single registered
     * {@link Location} code.
     */
    public static ElementPlacement<String> raw(String code, Point point) {
        return new ElementPlacement<>(code, point);
    }

    /**
     * Universal string code for action commands, labels and comparisons. Works the same way regardless of whether this
     * element was built via {@link #of} or {@link #raw}.
     */
    public String code() {
        if (key instanceof Location loc)
            return loc.getCode();
        return key != null ? key.toString() : "";
    }
}
