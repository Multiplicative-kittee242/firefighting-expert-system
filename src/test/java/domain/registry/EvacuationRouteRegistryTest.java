package domain.registry;

import domain.EvacuationRoute;
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
 * Tests {@link EvacuationRouteRegistry} construction, case-insensitive lookup,
 * directed route semantics, and rejection of malformed codes.
 */
class EvacuationRouteRegistryTest {
    // R/T stand in for the dead-end tank compartments that are legitimate evacuation targets.
    private static final LocationRegistry LOCATION_REGISTRY = TestLocations.registryOf("A", "B", "D", "H", "R", "T");

    @Test
    void resolves_PreservesInputOrderAsEscapeDirection() {
        final String code = "BA";

        EvacuationRouteRegistry registry = new EvacuationRouteRegistry(List.of(code), LOCATION_REGISTRY);
        EvacuationRoute route = registry.get(code);

        assertThat("from is the escape source, NOT normalized alphabetically", route.getFrom().getCode(), is("b"));
        assertThat(route.getTo().getCode(), is("a"));
        assertThat(route.getCode(), is(code));
    }

    @Test
    void resolves_OppositeDirectionsAreTwoDistinctRoutes() {
        final String routeDtoH = "DH";
        final String routeHtoD = "HD";
        final List<String> routes = List.of(routeDtoH, routeHtoD);

        EvacuationRouteRegistry registry = new EvacuationRouteRegistry(routes, LOCATION_REGISTRY);

        assertThat(registry.get(routeDtoH).getTo().getCode(), is("h"));
        assertThat(registry.get(routeHtoD).getTo().getCode(), is("d"));
        assertThat(registry.get(routeDtoH), is(not(registry.get(routeHtoD))));
        assertThat(registry.all(), hasSize(routes.size()));
    }

    @Test
    void resolves_RoutesIntoDeadEndTankCompartments() {
        final String routeToR = "DR";
        final String routeToT = "DT";

        EvacuationRouteRegistry registry = new EvacuationRouteRegistry(List.of(routeToR, routeToT), LOCATION_REGISTRY);

        assertThat(registry.get(routeToR).getTo(), sameInstance(LOCATION_REGISTRY.get("R")));
        assertThat(registry.get(routeToT).getTo(), sameInstance(LOCATION_REGISTRY.get("T")));
    }

    @Test
    void routesFrom_ReturnsAllOutgoingEscapeEdges() {
        final String routeDtoH = "DH";
        final String routeDtoR = "DR";
        final String routeDtoT = "DT";
        final String routeHtoD = "HD";

        EvacuationRouteRegistry registry =
            new EvacuationRouteRegistry(List.of(routeDtoH, routeDtoR, routeDtoT, routeHtoD), LOCATION_REGISTRY);

        List<String> fromD = registry.routesFrom(LOCATION_REGISTRY.get("D")).stream()
            .map(EvacuationRoute::getCode).toList();

        assertThat(fromD, contains(routeDtoH, routeDtoR, routeDtoT));
    }

    @Test
    void get_IsCaseInsensitiveAndReturnsCanonicalInstance() {
        final String code = "AB";

        EvacuationRouteRegistry registry = new EvacuationRouteRegistry(List.of(code), LOCATION_REGISTRY);

        assertThat(registry.get(code.toLowerCase()), sameInstance(registry.get(code)));
    }

    @ParameterizedTest(name = "\"{0}\" rejected: {1}")
    @CsvSource({
        "AA,  self-route (from equals to)",
        "ABD, code longer than two characters",
        "A,   code shorter than two characters",
        "AZ,  endpoint not in the location registry"
    })
    void resolves_RejectsMalformedOrUnresolvableCode(String code, String reason) {
        assertThrows(IllegalArgumentException.class,
            () -> new EvacuationRouteRegistry(List.of(code), LOCATION_REGISTRY));
    }

    @Test
    void resolves_DuplicateDirectedCodeFailsFast() {
        final String code = "AB";

        assertThrows(IllegalStateException.class,
            () -> new EvacuationRouteRegistry(List.of(code, code.toLowerCase()), LOCATION_REGISTRY));
    }

    @Test
    void resolves_ReversedCodeIsNotADuplicate() {
        final List<String> routes = List.of("AB", "BA");

        EvacuationRouteRegistry registry = new EvacuationRouteRegistry(routes, LOCATION_REGISTRY);

        assertThat(registry.all(), hasSize(routes.size()));
    }

    @Test
    void worksAsHashMapKeyByValue() {
        final String code = "AB";

        EvacuationRouteRegistry registry = new EvacuationRouteRegistry(List.of(code), LOCATION_REGISTRY);
        Map<EvacuationRoute, Integer> map = new HashMap<>();
        map.put(registry.get(code), 1);

        assertThat(map.get(registry.get(code.toLowerCase())), is(1));
    }

    @Test
    void all_IsUnmodifiable() {
        final String code = "AB";

        EvacuationRouteRegistry registry = new EvacuationRouteRegistry(List.of(code), LOCATION_REGISTRY);

        assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
    }
}
