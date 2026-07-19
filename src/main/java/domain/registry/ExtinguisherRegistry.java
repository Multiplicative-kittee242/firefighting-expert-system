package domain.registry;

import domain.Extinguisher;
import domain.Location;
import domain.types.ExtinguisherType;

import java.util.Map;

/**
 * Read-only in-memory registry of {@link Extinguisher} value objects, built once from a map of raw extinguisher title
 * to {@link ExtinguisherType}. Depends on a {@link LocationRegistry} to resolve each extinguisher's owning
 * {@link Location}, derived from the title's {@code est_<location><digit?>} naming convention — the same title-prefix
 * scheme {@link HydrantOutletsRegistry} uses for {@code hydr_…}. Titles are normalized to lower case; lookup is
 * case-insensitive; duplicate titles fail fast.
 */
public final class ExtinguisherRegistry extends TitlePrefixedLocationRegistry<Extinguisher> {
    private static final String TITLE_PREFIX = "est_";

    public ExtinguisherRegistry(Map<String, ExtinguisherType> extinguisherTypes, LocationRegistry locations) {
        super(TITLE_PREFIX, "extinguisher");
        buildFromTitleMap(extinguisherTypes, locations, Extinguisher::new);
    }
}
