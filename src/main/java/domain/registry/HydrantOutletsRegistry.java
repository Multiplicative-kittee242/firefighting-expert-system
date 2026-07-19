package domain.registry;

import domain.HydrantOutlets;
import domain.Location;

import java.util.Map;

/**
 * Read-only in-memory registry of {@link HydrantOutlets} value objects, built once from a map of raw hydrant title to
 * outlet count. Depends on a {@link LocationRegistry} to resolve each hydrant's owning {@link Location}, derived from
 * the title's {@code hydr_<location><digit?>} naming convention (same title-prefix scheme as
 * {@link ExtinguisherRegistry}'s {@code est_…}). Titles are normalized to lower case; lookup is case-insensitive;
 * duplicate titles fail fast.
 */
public final class HydrantOutletsRegistry extends TitlePrefixedLocationRegistry<HydrantOutlets> {
    private static final String TITLE_PREFIX = "hydr_";

    public HydrantOutletsRegistry(Map<String, Integer> hydrantOutlets, LocationRegistry locations) {
        super(TITLE_PREFIX, "hydrant");
        buildFromTitleMap(hydrantOutlets, locations, HydrantOutlets::new);
    }
}
