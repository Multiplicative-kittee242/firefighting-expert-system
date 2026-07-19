package fixtures;

import domain.registry.LocationRegistry;
import domain.registry.LocationRegistry.RawLocation;

import java.util.Arrays;
import java.util.List;

/**
 * Shared fixtures for tests that need a {@link LocationRegistry} of plain identity-only locations
 * (every scenario attribute at its default) as a <em>dependency</em> — not as the thing under test.
 * Centralizes the {@code List.of(codes).map(RawLocation::identity)} idiom that would otherwise be
 * copied into every registry test.
 * <p>
 * Deliberately not used by {@code LocationRegistryTest} itself: a class under test constructs its
 * own subject directly, so the test reads without indirection through a helper that wraps it.
 */
public final class TestLocations {
    private TestLocations() {}

    /** Identity-only {@link RawLocation} specs for the given codes, in order. */
    public static List<RawLocation> identities(String... codes) {
        return Arrays.stream(codes).map(RawLocation::identity).toList();
    }

    /** A {@link LocationRegistry} of identity-only locations for the given codes. */
    public static LocationRegistry registryOf(String... codes) {
        return new LocationRegistry(identities(codes));
    }
}
