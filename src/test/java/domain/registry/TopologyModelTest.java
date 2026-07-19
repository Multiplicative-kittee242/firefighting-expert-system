package domain.registry;

import domain.Border;
import domain.Door;
import domain.Extinguisher;
import domain.FireHoseSpan;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;
import domain.registry.BorderRegistry.RawBorder;
import domain.registry.DoorRegistry.RawDoor;
import domain.registry.FireHoseSpanRegistry.RawSpan;
import domain.registry.LocationRegistry.RawLocation;
import domain.types.ExtinguisherType;
import domain.types.FireSensorType;
import fixtures.TestLocations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@link TopologyModel} correctly resolves all six domain object types from the
 * underlying registries and that the aggregated views (all*()) are immutable.
 */
class TopologyModelTest {
    private static final List<RawLocation> LOCATIONS = TestLocations.identities("A", "B", "D", "F");
    private static final List<String> LINK_CODES = List.of("AB", "DF");
    private static final Map<String, FireSensorType> SENSOR_TYPES = Map.of("A1", FireSensorType.COMBINED, "D2", FireSensorType.COMBINED);
    private static final Map<String, Integer> HYDRANT_OUTLET_COUNTS = Map.of("hydr_f", 1, "hydr_d1", 2);
    private static final List<String> EVACUATION_ROUTE_CODES = List.of("AB", "BA", "DF");
    private static final List<RawSpan> DOOR_TO_DOOR_SPANS = List.of(new RawSpan("AB", "DF", 6.0));
    private static final List<RawSpan> HYDRANT_TO_DOOR_SPANS = List.of(new RawSpan("hydr_f", "DF", 2.7));
    private static final Map<String, ExtinguisherType> EXTINGUISHER_TYPES = Map.of("est_d", ExtinguisherType.CARBON_DIOXIDE);
    private static final List<RawBorder> RAW_BORDERS = List.of(new RawBorder("AB", 5.0));
    // DOOR_INTERIOR reuses the DF link code already registered above; DOOR_EXTERIOR exits the deck.
    private static final RawDoor DOOR_INTERIOR = new RawDoor("D", "F");
    private static final RawDoor DOOR_EXTERIOR = new RawDoor("F", Location.OUT.getCode());
    private static final List<RawDoor> RAW_DOORS = List.of(DOOR_INTERIOR, DOOR_EXTERIOR);

    private static final TopologyModel TOPOLOGY_MODEL = TopologyModel.from(new TopologyModel.RawTopology(
        LOCATIONS, LINK_CODES, SENSOR_TYPES, HYDRANT_OUTLET_COUNTS, EVACUATION_ROUTE_CODES,
        DOOR_TO_DOOR_SPANS, HYDRANT_TO_DOOR_SPANS, EXTINGUISHER_TYPES, RAW_BORDERS, RAW_DOORS));

    @Test
    void locationInstance_ofDomainTypes() {
        TopologyModel model = TOPOLOGY_MODEL;
        assertThat(model.location("A").getCode(), is("a"));
        assertThat(model.link("ab").getCode(), is("AB"));
        assertThat(model.fireSensor("a1").getCode(), is("A1"));
        assertThat(model.hydrantOutlets("HYDR_F").getTitle(), is("hydr_f"));
        assertThat(model.evacuationRoute("ab").getFrom().getCode(), is("a"));
        assertThat(model.extinguisher("EST_D").getTitle(), is("est_d"));
    }

    @Test
    void locationInstance_ofFireHoseSpans() {
        TopologyModel model = TOPOLOGY_MODEL;
        Link doorDF = model.link("DF");

        List<FireHoseSpan<Link>> doorToDoor = model.allDoorToDoorFireHoseSpans();
        assertThat(doorToDoor, hasSize(DOOR_TO_DOOR_SPANS.size()));
        assertThat(doorToDoor.get(0).getFrom(), sameInstance(model.link("AB")));
        assertThat(doorToDoor.get(0).getTo(), sameInstance(doorDF));

        List<FireHoseSpan<HydrantOutlets>> hydrantToDoor = model.allHydrantToDoorFireHoseSpans();
        assertThat(hydrantToDoor, hasSize(HYDRANT_TO_DOOR_SPANS.size()));
        assertThat(hydrantToDoor.get(0).getFrom(), sameInstance(model.hydrantOutlets("hydr_f")));
        assertThat(hydrantToDoor.get(0).getTo(), sameInstance(doorDF));
    }

    @Test
    void locationInstance_ofEvacuationRoutes() {
        TopologyModel model = TOPOLOGY_MODEL;
        // AB (a→b) and BA (b→a) are distinct directed routes, not one normalized edge.
        assertThat(model.evacuationRoute("AB").getTo(), sameInstance(model.location("B")));
        assertThat(model.evacuationRoute("BA").getTo(), sameInstance(model.location("A")));
        assertThat(model.allEvacuationRoutes(), hasSize(EVACUATION_ROUTE_CODES.size()));
    }

    @Test
    void locationInstance_ofLinksAndSensors() {
        TopologyModel model = TOPOLOGY_MODEL;
        Location a = model.location("A");
        assertThat(model.link("AB").getFrom(), sameInstance(a));
        assertThat(model.fireSensor("A1").getLocation(), sameInstance(a));
    }

    @Test
    void locationInstance_ofHydrantOutlets() {
        final String hydrantTitle = "hydr_d1";

        TopologyModel model = TOPOLOGY_MODEL;
        HydrantOutlets hydrant = model.hydrantOutlets(hydrantTitle);

        assertThat(hydrant.getLocation(), sameInstance(model.location("D")));
        assertThat(hydrant.getOutlets(), is(HYDRANT_OUTLET_COUNTS.get(hydrantTitle)));
    }

    @Test
    void locationInstance_ofExtinguishers() {
        final String title = "est_d";

        TopologyModel model = TOPOLOGY_MODEL;
        Extinguisher extinguisher = model.extinguisher(title);

        assertThat(extinguisher.getLocation(), sameInstance(model.location("D")));
        assertThat(extinguisher.getType(), is(EXTINGUISHER_TYPES.get(title)));
    }

    @Test
    void exposesImmutableAggregatedViews() {
        TopologyModel model = TOPOLOGY_MODEL;
        assertThat(model.allLocations(), hasSize(LOCATIONS.size()));
        assertThat(model.allLinks(), hasSize(LINK_CODES.size()));
        assertThat(model.allFireSensors(), hasSize(SENSOR_TYPES.size()));
        assertThat(model.allHydrantOutlets(), hasSize(HYDRANT_OUTLET_COUNTS.size()));
        assertThat(model.allEvacuationRoutes(), hasSize(EVACUATION_ROUTE_CODES.size()));
        assertThat(model.allExtinguishers(), hasSize(EXTINGUISHER_TYPES.size()));
        assertThat(model.allDoorToDoorFireHoseSpans(), hasSize(DOOR_TO_DOOR_SPANS.size()));
        assertThat(model.allHydrantToDoorFireHoseSpans(), hasSize(HYDRANT_TO_DOOR_SPANS.size()));
        assertThat(model.allBorders(), hasSize(RAW_BORDERS.size()));
        assertThat(model.allDoors(), hasSize(RAW_DOORS.size()));

        assertThrows(UnsupportedOperationException.class, () -> model.allLocations().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allLinks().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allFireSensors().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allHydrantOutlets().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allEvacuationRoutes().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allExtinguishers().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allDoorToDoorFireHoseSpans().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allHydrantToDoorFireHoseSpans().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allBorders().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.allDoors().clear());
    }

    @Test
    void allBorders_ResolvesLinkIdentityAndLength() {
        TopologyModel model = TOPOLOGY_MODEL;

        Border border = model.allBorders().get(0);

        assertThat(border.getLink(), sameInstance(model.link("AB")));
        assertThat(border.getLength(), is(RAW_BORDERS.get(0).length()));
    }

    @Test
    void allDoors_ResolvesLocationEndpointsAndTheExternalDeckSentinel() {
        TopologyModel model = TOPOLOGY_MODEL;
        List<Door> doors = model.allDoors();

        Door interiorDoor = doors.get(0);
        assertThat(interiorDoor.getFrom(), sameInstance(model.location("D")));
        assertThat(interiorDoor.getTo(), sameInstance(model.location("F")));

        // DOOR_EXTERIOR's "to" is the Location.OUT sentinel, not a real registered location.
        Door exteriorDoor = doors.get(1);
        assertThat(exteriorDoor.getFrom(), sameInstance(model.location("F")));
        assertThat(exteriorDoor.getTo(), sameInstance(Location.OUT));
    }
}
