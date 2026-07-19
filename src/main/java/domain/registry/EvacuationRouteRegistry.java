package domain.registry;

import domain.EvacuationRoute;
import domain.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only in-memory registry of {@link EvacuationRoute} value objects, built once from a list of two-character
 * directed codes ({@code from} -then- {@code to}, e.g. {@code "EF "} for {@code e→f}). Depends on a
 * {@link LocationRegistry} to resolve each endpoint — the same explicit ordering dependency the other registries carry
 * (locations first).
 * <p>
 * Structurally the directed sibling of {@link LinkRegistry}: the code is uppercased and must be exactly two characters,
 * and a route may not connect a location to itself. The one deliberate difference is that the endpoints are
 * <em>not</em> reordered alphabetically — the input order is the escape direction and is preserved, so {@code e→f} and
 * {@code f→e} are two distinct routes. Lookup is case-insensitive; duplicate directed codes fail fast. Construction /
 * lookup shell is shared with {@link LinkRegistry} via {@link TwoCharEndpointRegistry}.
 */
public final class EvacuationRouteRegistry extends TwoCharEndpointRegistry<EvacuationRoute> {

    public EvacuationRouteRegistry(List<String> rawDirectedCodes, LocationRegistry locations) {
        super("evacuation route");
        buildFromCodes(rawDirectedCodes, locations, (code, from, to) -> new EvacuationRoute(from, to));
    }

    @Override
    protected OrderedEndpoints orderEndpoints(String c1, String c2, Location loc1, Location loc2) {
        return new OrderedEndpoints(loc1, loc2);
    }

    /**
     * All routes whose {@code from} endpoint is the given location — the escape edges leading out of it. Convenience
     * adjacency view for rendering / graph traversal in the Java layer.
     */
    public List<EvacuationRoute> routesFrom(Location from) {
        List<EvacuationRoute> routes = new ArrayList<>();
        for (EvacuationRoute route : all()) {
            if (route.getFrom().equals(from))
                routes.add(route);
        }
        return Collections.unmodifiableList(routes);
    }
}
