package domain.registry;

import domain.Link;
import domain.Location;

import java.util.List;

/**
 * Read-only in-memory registry of {@link Link} value objects, built once from a list of two-character border-link
 * codes. Depends on a {@link LocationRegistry} to resolve each link's endpoints — an explicit constructor dependency
 * that replaces the former implicit ordering requirement of the static {@code initializeAll} calls (locations first).
 * <p>
 * Normalization rules (preserved exactly): the code is uppercased and must be exactly two characters; a link may not
 * connect a location to itself; {@code from}/{@code to} are ordered so {@code from} is alphabetically earlier. Lookup
 * is case-insensitive; duplicate codes fail fast. Construction / lookup shell is shared with
 * {@link EvacuationRouteRegistry} via {@link TwoCharEndpointRegistry}; the deliberate difference is endpoint ordering.
 */
public final class LinkRegistry extends TwoCharEndpointRegistry<Link> {

    public LinkRegistry(List<String> rawCodes, LocationRegistry locations) {
        super("link");
        buildFromCodes(rawCodes, locations, Link::new);
    }

    @Override
    protected OrderedEndpoints orderEndpoints(String c1, String c2, Location loc1, Location loc2) {
        boolean firstIsEarlier = c1.compareToIgnoreCase(c2) <= 0;
        Location fromLoc = firstIsEarlier ? loc1 : loc2;
        Location toLoc = firstIsEarlier ? loc2 : loc1;
        return new OrderedEndpoints(fromLoc, toLoc);
    }
}
