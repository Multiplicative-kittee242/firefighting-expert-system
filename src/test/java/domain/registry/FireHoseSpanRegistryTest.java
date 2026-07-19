package domain.registry;

import domain.FireHoseSpan;
import domain.HydrantOutlets;
import domain.Link;
import domain.registry.FireHoseSpanRegistry.RawSpan;
import fixtures.TestLocations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link FireHoseSpanRegistry}, in particular the order-insensitive door-endpoint
 * resolution that fixes two data bugs discovered in the original {@code feis.clp}
 * {@code FIRE-DISTANCE} definstances: a reversed door endpoint (e.g. authored as {@code "HG"}
 * when the registered door is {@code "GH"}) and a door value collapsed into a single malformed
 * token instead of two — both of which made the CLIPS rule base's {@code arrange-letters}-based
 * matching permanently fail to find those two hose-reach distances.
 */
class FireHoseSpanRegistryTest {
    private static final LocationRegistry LOCATIONS = TestLocations.registryOf("D", "E", "G", "H");
    private static final LinkRegistry LINKS = new LinkRegistry(List.of("DE", "GH"), LOCATIONS);
    private static final HydrantOutletsRegistry HYDRANTS =
        new HydrantOutletsRegistry(Map.of("hydr_d1", 2), LOCATIONS);

    @Test
    void resolves_DoorToDoorSpanEndpointsViaLinkRegistry() {
        final String from = "DE";
        final String to = "GH";
        final double distance = 12.0;

        FireHoseSpanRegistry registry = new FireHoseSpanRegistry(
            List.of(new RawSpan(from, to, distance)), List.of(), LINKS, HYDRANTS);

        FireHoseSpan<Link> span = registry.allDoorToDoor().get(0);
        assertThat(span.getFrom(), sameInstance(LINKS.get(from)));
        assertThat(span.getTo(), sameInstance(LINKS.get(to)));
        assertThat(span.getDistance(), is(distance));
    }

    @Test
    void resolves_DoorEndpointResolutionIsOrderInsensitive() {
        // The registered door is "GH"; authoring it reversed as "HG" must still resolve to it —
        // this is exactly the shape of the dh_to_hg bug in the original feis.clp data.
        FireHoseSpanRegistry registry = new FireHoseSpanRegistry(
            List.of(new RawSpan("DE", "HG", 2.0)), List.of(), LINKS, HYDRANTS);

        assertThat(registry.allDoorToDoor().get(0).getTo(), sameInstance(LINKS.get("GH")));
    }

    @Test
    void resolves_HydrantToDoorSpanEndpoints() {
        final String from = "hydr_d1";
        final String to = "DE";
        final double distance = 1.6;

        FireHoseSpanRegistry registry = new FireHoseSpanRegistry(
            List.of(), List.of(new RawSpan(from, to, distance)), LINKS, HYDRANTS);

        FireHoseSpan<HydrantOutlets> span = registry.allHydrantToDoor().get(0);
        assertThat(span.getFrom(), sameInstance(HYDRANTS.get(from)));
        assertThat(span.getTo(), sameInstance(LINKS.get(to)));
        assertThat(span.getDistance(), is(distance));
    }

    @ParameterizedTest(name = "door code \"{0}\" rejected: {1}")
    @CsvSource({
        "ZZ,  endpoint not in the link registry",
        "GHI, code not exactly two characters"
    })
    void resolves_RejectsMalformedOrUnresolvableDoorCode(String doorCode, String reason) {
        assertThrows(IllegalArgumentException.class, () -> new FireHoseSpanRegistry(
            List.of(new RawSpan("DE", doorCode, 1.0)), List.of(), LINKS, HYDRANTS));
    }

    @Test
    void resolves_RejectsUnknownHydrantTitle() {
        assertThrows(IllegalArgumentException.class, () -> new FireHoseSpanRegistry(
            List.of(), List.of(new RawSpan("hydr_x", "DE", 1.0)), LINKS, HYDRANTS));
    }

    @Test
    void unmodifiableAndOrderedLists() {
        List<RawSpan> doorToDoorSpans = List.of(new RawSpan("DE", "GH", 1.0), new RawSpan("GH", "DE", 2.0));
        List<RawSpan> hydrantToDoorSpans = List.of(new RawSpan("hydr_d1", "DE", 3.0));

        FireHoseSpanRegistry registry = new FireHoseSpanRegistry(doorToDoorSpans, hydrantToDoorSpans, LINKS, HYDRANTS);

        assertThat(registry.allDoorToDoor(), hasSize(doorToDoorSpans.size()));
        assertThat(registry.allHydrantToDoor(), hasSize(hydrantToDoorSpans.size()));
        assertThrows(UnsupportedOperationException.class, () -> registry.allDoorToDoor().clear());
        assertThrows(UnsupportedOperationException.class, () -> registry.allHydrantToDoor().clear());
    }

    @Test
    void valueEquality_HoldsAcrossIndependentInstancesIgnoringDistance() {
        final String from = "DE";
        final String to = "GH";

        Link fromLink = LINKS.get(from);
        Link toLink = LINKS.get(to);

        FireHoseSpan<Link> one = new FireHoseSpan<>(fromLink, toLink, 1.0);
        FireHoseSpan<Link> two = new FireHoseSpan<>(fromLink, toLink, 99.9);

        assertThat(two, equalTo(one));
        assertThat(two.hashCode(), is(one.hashCode()));
    }

    @Test
    void worksAsHashMapKeyByValue() {
        final String from = "DE";
        final String to = "GH";

        Link fromLink = LINKS.get(from);
        Link toLink = LINKS.get(to);

        Map<FireHoseSpan<Link>, Double> map = new HashMap<>();
        map.put(new FireHoseSpan<>(fromLink, toLink, 5.0), 5.0);

        assertThat(map.get(new FireHoseSpan<>(fromLink, toLink, 99.0)), is(5.0));
    }
}
