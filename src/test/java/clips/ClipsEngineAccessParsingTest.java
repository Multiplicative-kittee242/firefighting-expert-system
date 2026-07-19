package clips;

import domain.Extinguisher;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import domain.types.ExtinguisherType;
import fixtures.TestLocations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the pure string-to-domain parsing helpers {@link ClipsEngineAccess} uses to turn raw
 * CLIPS query responses into domain objects: {@link ClipsEngineAccess#parseLocations}, {@link
 * ClipsEngineAccess#parseHydrantOutlets}, {@link ClipsEngineAccess#parseExtinguishers}, {@link
 * ClipsEngineAccess#parseLocationLinks}. Like {@link ClipsEngineAccessTest}'s coverage of the
 * opposite (domain-to-string) direction, these are {@code static} and take {@link TopologyModel}
 * explicitly precisely so they are unit-testable here, on a normal JVM, without the real 32-bit
 * CLIPS engine — {@link ClipsEngineAccess} itself cannot be constructed without one (its
 * constructor loads the native rule base), so these stay {@code static} rather than becoming
 * instance methods over the {@link TopologyModel} field it now holds.
 */
class ClipsEngineAccessParsingTest {
    private static final TopologyModel TOPOLOGY = TopologyModel.from(
        TopologyModel.RawTopology.empty()
            .withLocations(TestLocations.identities("A", "B", "D"))
            .withLinkCodes(List.of("AB", "BD"))
            .withHydrantOutletCounts(Map.of("hydr_a", 2, "hydr_b", 1))
            .withExtinguisherTypes(Map.of(
                "est_a", ExtinguisherType.CARBON_DIOXIDE,
                "est_b", ExtinguisherType.AIR_FOAM)));

    @Test
    void parseLocations_ResolvesEachCharacterAsALocationCode() {
        Set<Location> locations = ClipsEngineAccess.parseLocations(TOPOLOGY, "ab");
        assertThat(locations, containsInAnyOrder(TOPOLOGY.location("a"), TOPOLOGY.location("b")));
    }

    @Test
    void parseLocations_EmptyStringYieldsEmptySet() {
        assertThat(ClipsEngineAccess.parseLocations(TOPOLOGY, ""), empty());
    }

    @Test
    void parseHydrantOutlets_ResolvesEachSpaceSeparatedTitleInOrder() {
        List<HydrantOutlets> hydrants = ClipsEngineAccess.parseHydrantOutlets(TOPOLOGY, "hydr_a hydr_b");
        assertThat(hydrants, contains(TOPOLOGY.hydrantOutlets("hydr_a"), TOPOLOGY.hydrantOutlets("hydr_b")));
    }

    @Test
    void parseHydrantOutlets_SkipsBlankTokensFromExtraSpaces() {
        List<HydrantOutlets> hydrants = ClipsEngineAccess.parseHydrantOutlets(TOPOLOGY, "  hydr_a   hydr_b  ");
        assertThat(hydrants, contains(TOPOLOGY.hydrantOutlets("hydr_a"), TOPOLOGY.hydrantOutlets("hydr_b")));
    }

    @Test
    void parseHydrantOutlets_NullYieldsEmptyList() {
        assertThat(ClipsEngineAccess.parseHydrantOutlets(TOPOLOGY, null), empty());
    }

    @Test
    void parseHydrantOutlets_BlankStringYieldsEmptyList() {
        assertThat(ClipsEngineAccess.parseHydrantOutlets(TOPOLOGY, "   "), empty());
    }

    @Test
    void parseExtinguishers_ResolvesEachSpaceSeparatedTitleInOrder() {
        List<Extinguisher> extinguishers = ClipsEngineAccess.parseExtinguishers(TOPOLOGY, "est_a est_b");
        assertThat(extinguishers, contains(TOPOLOGY.extinguisher("est_a"), TOPOLOGY.extinguisher("est_b")));
    }

    @Test
    void parseExtinguishers_NullYieldsEmptyList() {
        assertThat(ClipsEngineAccess.parseExtinguishers(TOPOLOGY, null), empty());
    }

    @Test
    void parseExtinguishers_BlankStringYieldsEmptyList() {
        assertThat(ClipsEngineAccess.parseExtinguishers(TOPOLOGY, ""), empty());
    }

    @Test
    void parseLocationLinks_ResolvesEachTwoCharacterCodeInOrder() {
        List<Link> links = ClipsEngineAccess.parseLocationLinks(TOPOLOGY, "abbd");
        assertThat(links, contains(TOPOLOGY.link("ab"), TOPOLOGY.link("bd")));
    }

    @Test
    void parseLocationLinks_EmptyStringYieldsEmptyList() {
        assertThat(ClipsEngineAccess.parseLocationLinks(TOPOLOGY, ""), empty());
    }

    /**
     * Currently unreachable from either real caller ({@code get-line1-borders}'s {@code BORDER}
     * endpoints are always real locations; {@code collect-germ-door}'s {@code to-close}/
     * {@code keep-open} statuses are never assigned to a door to {@link Location#OUT} — see
     * {@link ClipsEngineAccess#parseLocationLinks}'s javadoc) — guarded anyway so a future
     * rule-base change that breaks that invariant fails loudly instead of silently dropping data.
     */
    @Test
    void parseLocationLinks_OddLengthInputThrows() {
        assertThrows(IllegalStateException.class, () -> ClipsEngineAccess.parseLocationLinks(TOPOLOGY, "abbda"));
    }
}
