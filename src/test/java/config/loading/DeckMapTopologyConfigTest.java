package config.loading;

import config.specification.basic.FireHoseSpansSpec;
import config.specification.basic.*;
import config.specification.basic.HydrantOutletSpec;
import domain.types.FlammableMaterial;
import domain.types.CompartmentType;
import domain.types.ExplosiveMaterial;
import domain.Extinguisher;
import domain.types.ExtinguisherType;
import domain.FireHoseSpan;
import domain.types.FireSensorType;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;
import domain.types.VentilationType;
import domain.registry.TopologyModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static fixtures.TopologyConfigBuilder.topologyConfig;

/**
 * Covers {@link DeckMapTopologyConfig#getFireRatedByDoorCode()} (the single source of truth for
 * the ordinary/fire-rated door rendering distinction, see {@code gui.map.input.controls.DoorSealingButton},
 * replacing the duplicate {@code style} field previously authored separately in {@code controls.yaml})
 * and {@link DeckMapTopologyConfig#buildTopologyModel()} (resolving this raw config into the
 * domain's {@link TopologyModel}, keeping that dependency on the {@code config} side per
 * {@code clips/README.md}'s layering).
 */
class DeckMapTopologyConfigTest {

    @Test
    void getFireRatedByDoorCode_KeysByConcatenatedFromToCode() {
        DeckMapTopologyConfig config = topologyConfig()
            .doors(new DoorSpec("A", "Q", false), new DoorSpec("A", "B", true))
            .build();

        Map<String, Boolean> fireRatedByCode = config.getFireRatedByDoorCode();

        assertThat(fireRatedByCode.get("AQ"), is(false));
        assertThat(fireRatedByCode.get("AB"), is(true));
    }

    @Test
    void getFireRatedByDoorCode_ExcludesDoorsToAnotherDeck() {
        DeckMapTopologyConfig config = topologyConfig()
            .doors( // sentinel matching is case-insensitive
                new DoorSpec("J", DoorSpec.EXTERNAL_DECK, true),
                new DoorSpec("J", DoorSpec.EXTERNAL_DECK, true))
            .build();

        Map<String, Boolean> fireRatedByCode = config.getFireRatedByDoorCode();

        assertThat("doors to another deck have no button/code to key by", fireRatedByCode.isEmpty(), is(true));
    }

    @Test
    void buildTopologyModel_ResolvesLocationsBordersHydrantsAndEvacuationRoutes() {
        final String borderCode = "AB";
        final String sensorCode = "A1";
        final String hydrantTitle = "hydr_a";
        final int outlets = 2;

        final List<LocationSpec> locationSpecs = List.of(
            LocationSpec.identity("A"),
            LocationSpec.identity("B"),
            LocationSpec.identity("D"));
        final List<EvacuationRouteSpec> evacRoutes = List.of(
            new EvacuationRouteSpec("A", "B"),
            new EvacuationRouteSpec("B", "A"));

        DeckMapTopologyConfig config = topologyConfig()
            .locations(locationSpecs.toArray(LocationSpec[]::new))
            .borders(new BorderSpec(borderCode, 2.4))
            .doors(
                new DoorSpec("A", "D", false),
                new DoorSpec("D", DoorSpec.EXTERNAL_DECK, true))
            .evacuationRoutes(evacRoutes.toArray(EvacuationRouteSpec[]::new))
            .sensors(new FireSensorSpec(sensorCode, FireSensorType.COMBINED.name()))
            .hydrants(new HydrantOutletSpec(hydrantTitle, outlets))
            .build();

        TopologyModel topology = config.buildTopologyModel();

        assertThat(topology.allLocations(), hasSize(locationSpecs.size()));
        assertThat(topology.link(borderCode).getFrom().getCode(), is("a"));
        assertThat(topology.link("AD").getTo().getCode(), is("d"));
        assertThat(topology.fireSensor(sensorCode).getLocation(), is(topology.location("A")));
        assertThat(topology.hydrantOutlets(hydrantTitle).getOutlets(), is(outlets));
        // Direction preserved from the from/to spec order, both edges present.
        assertThat(topology.evacuationRoute("AB").getTo().getCode(), is("b"));
        assertThat(topology.evacuationRoute("BA").getTo().getCode(), is("a"));
        assertThat(topology.allEvacuationRoutes(), hasSize(evacRoutes.size()));
    }

    @Test
    void buildTopologyModel_ExcludesDoorsToAnotherDeckFromLinkCodes() {
        final List<LocationSpec> locationSpecs = List.of(
            LocationSpec.identity("D"), LocationSpec.identity("J"));

        DeckMapTopologyConfig config = topologyConfig()
            .locations(locationSpecs.toArray(LocationSpec[]::new))
            .doors(new DoorSpec("J", DoorSpec.EXTERNAL_DECK, false))
            .build();

        TopologyModel topology = config.buildTopologyModel();

        assertThat("doors to another deck produce no interior links", topology.allLinks(), hasSize(0));
    }

    @Test
    void buildTopologyModel_ResolvesDoorToDoorAndHydrantToDoorFireHoseSpans() {
        final String doorAB = "AB";
        final String doorBD = "BD";
        final String hydrantTitle = "hydr_a";
        final double doorToDoorDistance = 3.5;
        final double hydrantToDoorDistance = 1.2;

        DeckMapTopologyConfig config = topologyConfig()
            .locations(LocationSpec.identity("A"), LocationSpec.identity("B"), LocationSpec.identity("D"))
            .borders(new BorderSpec(doorAB, 2.4), new BorderSpec(doorBD, 1.0))
            .fireHoseSpans(new FireHoseSpansSpec(
                List.of(new FireHoseSpanSpec(doorAB, doorBD, doorToDoorDistance)),
                List.of(new FireHoseSpanSpec(hydrantTitle, doorAB, hydrantToDoorDistance))))
            .hydrants(new HydrantOutletSpec(hydrantTitle, 2))
            .build();

        TopologyModel topology = config.buildTopologyModel();

        List<FireHoseSpan<Link>> doorToDoor = topology.allDoorToDoorFireHoseSpans();
        assertThat("one door-to-door span from the provided spec", doorToDoor, hasSize(1));
        assertThat(doorToDoor.get(0).getFrom().getCode(), is(doorAB));
        assertThat(doorToDoor.get(0).getTo().getCode(), is(doorBD));
        assertThat(doorToDoor.get(0).getDistance(), is(doorToDoorDistance));

        List<FireHoseSpan<HydrantOutlets>> hydrantToDoor = topology.allHydrantToDoorFireHoseSpans();
        assertThat("one hydrant-to-door span from the provided spec", hydrantToDoor, hasSize(1));
        assertThat(hydrantToDoor.get(0).getFrom().getTitle(), is(hydrantTitle));
        assertThat(hydrantToDoor.get(0).getTo().getCode(), is(doorAB));
    }

    @Test
    void buildTopologyModel_ResolvesExtinguisherTypesFromClipsValueStrings() {
        final String titleA = "est_a";
        final String titleB = "est_b1";

        DeckMapTopologyConfig config = topologyConfig()
            .locations(LocationSpec.identity("A"), LocationSpec.identity("B"))
            .extinguishers(
                new ExtinguisherSpec(titleA, ExtinguisherType.CARBON_DIOXIDE.name()),
                new ExtinguisherSpec(titleB, ExtinguisherType.AIR_FOAM.name()))
            .build();

        TopologyModel topology = config.buildTopologyModel();

        List<Extinguisher> extinguishers = topology.allExtinguishers();
        assertThat("two extinguishers from the provided specs", extinguishers, hasSize(2));
        assertThat(topology.extinguisher(titleA).getType(), is(ExtinguisherType.CARBON_DIOXIDE));
        assertThat(topology.extinguisher(titleB).getType(), is(ExtinguisherType.AIR_FOAM));
        assertThat(topology.extinguisher(titleB).getLocation(), is(topology.location("B")));
    }

    @Test
    void buildTopologyModel_ResolvesLocationScenarioAttributesFromClipsValueStrings() {
        final String codeE = "E";
        final String codeR = "R";
        LocationSpec e = new LocationSpec(codeE, 31.0, null, CompartmentType.AUXILIARY.name(),
            VentilationType.SMOKE_CONTROL.name(), ExplosiveMaterial.DIESEL_OIL.name(), FlammableMaterial.MACHINE_OIL.name(), true, true);
        LocationSpec r = new LocationSpec(codeR, null, 4, null, null, null, null, null, null);
        DeckMapTopologyConfig config = topologyConfig().locations(e, r).build();

        TopologyModel topology = config.buildTopologyModel();

        Location locationE = topology.location(codeE);
        assertThat(locationE.getArea(), is(31.0));
        assertThat(locationE.getTank(), is(Location.DEFAULT_TANK));
        assertThat(locationE.getType(), is(CompartmentType.AUXILIARY));
        assertThat(locationE.getVentilationType(), is(Optional.of(VentilationType.SMOKE_CONTROL)));
        assertThat(locationE.getExplosiveMaterial(), is(Optional.of(ExplosiveMaterial.DIESEL_OIL)));
        assertThat(locationE.getBurningMaterial(), is(Optional.of(FlammableMaterial.MACHINE_OIL)));
        assertThat(locationE.hasMachinery(), is(true));
        assertThat(locationE.hasChemicalSuppression(), is(true));

        Location locationR = topology.location(codeR);
        assertThat(locationR.getArea(), is(Location.NO_AREA));
        assertThat(locationR.getTank(), is(4));
        assertThat(locationR.getType(), is(CompartmentType.UNINHABITED));
        assertThat(locationR.getVentilationType().isPresent(), is(false));
        assertThat(locationR.getExplosiveMaterial().isPresent(), is(false));
    }

    @Test
    void buildTopologyModel_ResolvesFireSensorTypesFromClipsValueStrings() {
        final String sensorA1 = "A1";
        final String sensorD3 = "D3";

        DeckMapTopologyConfig config = topologyConfig()
            .locations(LocationSpec.identity("A"), LocationSpec.identity("D"))
            .sensors(new FireSensorSpec(sensorA1, FireSensorType.COMBINED.name()), new FireSensorSpec(sensorD3, FireSensorType.RATE_OF_RISE.name()))
            .build();

        TopologyModel topology = config.buildTopologyModel();

        assertThat(topology.fireSensor(sensorA1).getType(), is(FireSensorType.COMBINED));
        assertThat(topology.fireSensor(sensorD3).getType(), is(FireSensorType.RATE_OF_RISE));
    }
}
