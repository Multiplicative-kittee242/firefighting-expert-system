package config.validation;

import config.loading.DeckMapControlsConfig;
import config.loading.DeckMapGroupsConfig;
import config.loading.DeckMapTopologyConfig;
import config.loading.DeckMapGeometryConfig;
import config.specification.*;
import config.groups.*;
import config.enums.HydrantLabelSize;
import config.specification.basic.*;
import config.specification.buttons.*;
import geometry.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests for cross-file integrity: nonexistent code in controls triggers descriptive exception;
 * fully consistent small fixture passes.
 */
class ConfigIntegrityCheckerTest {

    @Test
    void check_NonexistentLocationInControlsPlacingYieldsExceptionWithFileAndField() {
        // minimal consistent topology
        List<LocationSpec> locs = List.of(
            LocationSpec.identity("A"),
            LocationSpec.identity("B")
        );
        DeckMapTopologyConfig topo = new DeckMapTopologyConfig(locs, List.of(), List.of(), List.of(),
            new FireHoseSpansSpec(List.of(), List.of()), List.of(), List.of(), List.of()
        );

        // controls referencing unknown location "Z"
        List<ElementPlacement<String>> badExplosion = List.of(ElementPlacement.raw("Z", new Point(10, 20)));
        DeckMapControlsConfig badControls = new DeckMapControlsConfig(badExplosion, List.of(), List.of(), List.of(), List.of());

        DeckMapGroupsConfig emptyGroups = makeEmptyGroups();
        DeckMapGeometryConfig emptyGeometry = makeEmptyGeometry();

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, badControls, emptyGroups, emptyGeometry));

        assertThat(ex.getViolations(), hasSize(greaterThanOrEqualTo(1)));
        String msg = ex.getMessage();
        assertThat(msg, containsString("config/controls.yaml"));
        assertThat(msg, containsString("explosion-markers-placing"));
        assertThat(msg, containsString("Z"));
        assertThat(msg, containsString("location not found in topology"));
    }

    @Test
    void check_ValidConsistentConfigsPassWithoutException() {
        List<LocationSpec> locs = List.of(
            LocationSpec.identity("A"),
            LocationSpec.identity("B")
        ); // 2 labels → expect 0 geometry polygons (2-2)
        List<BorderSpec> borders = List.of(); // empty for minimal test to satisfy border exact match
        List<DoorSpec> doors = List.of(
            new DoorSpec("A", "B", false)
        );
        DeckMapTopologyConfig topo = new DeckMapTopologyConfig(locs, borders, doors, List.of(),
            new FireHoseSpansSpec(List.of(), List.of()), List.of(), List.of(), List.of()
        );
        List<ElementPlacement<String>> places = List.of(
            ElementPlacement.raw("A", new Point(1, 2))
        );
        DeckMapControlsConfig controls = new DeckMapControlsConfig(
            places, places, places, List.of(), List.of()
        );

        DeckMapGroupsConfig groups = makeEmptyGroups();
        // match: tank 3 with 2 locations, both have coords (or none). Use empty for simplicity in this minimal test.
        DeckMapGeometryConfig geometry = new DeckMapGeometryConfig(List.of(), List.of());

        assertDoesNotThrow(() -> ConfigIntegrityChecker.check(topo, controls, groups, geometry));
    }

    @Test
    void check_GeometryLocationCountMismatchReportsFileAndExpectedActual() {
        // Two locations from the same tank (tank 3: A and B), but geometry provides only one → partial match violation
        List<LocationSpec> locs = List.of(
            new LocationSpec("A", null, 3, null, null, null, null, null, null),
            new LocationSpec("B", null, 3, null, null, null, null, null, null)
        );
        DeckMapTopologyConfig topo = new DeckMapTopologyConfig(locs, List.of(), List.of(), List.of(),
            new FireHoseSpansSpec(List.of(), List.of()), List.of(), List.of(), List.of()
        );
        DeckMapControlsConfig controls = new DeckMapControlsConfig(
            List.of(), List.of(), List.of(), List.of(), List.of()
        );
        DeckMapGroupsConfig groups = makeEmptyGroups();
        // Only one coordinate for tank 3 (partial)
        DeckMapGeometryConfig badGeo = new DeckMapGeometryConfig(
            List.of(new LocationCoordinateSpec("A", List.of())),
            List.of()
        );

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, controls, groups, badGeo));

        String msg = ex.getMessage();
        assertThat(msg, containsString("config/geometry.yaml"));
        assertThat(msg, containsString("tank 3"));
        assertThat(msg, containsString("do not exactly match"));
    }

    private static DeckMapGroupsConfig makeEmptyGroups() {
        // minimal groups that satisfy canonical constructor (typed to match record components)
        ToggleGroupConfig<VentilationButtonSpec> emptyVent = new ToggleGroupConfig<>(List.of(), null);
        ToggleGroupConfig<FlammableButtonSpec> emptyFlame = new ToggleGroupConfig<>(List.of(), null);
        ToggleGroupConfig<MachineryDamageButtonSpec> emptyMach = new ToggleGroupConfig<>(List.of(), null);
        ToggleGroupConfig<FireSensorButtonSpec> emptySensor = new ToggleGroupConfig<>(List.of(), null);
        ToggleGroupConfig<ExplosionButtonSpec> emptyExpl = new ToggleGroupConfig<>(List.of(), null);
        HydrantsGroupConfig<FrontlineBalanceGroupSpec> emptyFront = new HydrantsGroupConfig<>(List.of(), 0, 0, 0);
        HydrantsGroupConfig<FirefightingStepGroupSpec> emptyFight = new HydrantsGroupConfig<>(List.of(), 0, 0, 0);
        HydrOutletLabelGroupConfig emptyLabels = new HydrOutletLabelGroupConfig(List.of(), 0, 0, 0);
        DoorButtonGroupConfig emptyDoor = new DoorButtonGroupConfig(List.of(), null);

        return new DeckMapGroupsConfig(
            emptyVent, emptyFlame, emptyMach, emptySensor, emptyExpl,
            emptyFront, emptyFight,
            emptyLabels,
            emptyDoor,
            List.of(), List.of()
        );
    }

    private static DeckMapGeometryConfig makeEmptyGeometry() {
        return new DeckMapGeometryConfig(List.of(), List.of());
    }

    // --- Group A: controls.yaml bad location codes, parameterized ---
    static Stream<Object[]> check_ControlsBadLocationCodeReportsFileAndSection() {
        return Stream.of(
            new Object[] { new DeckMapControlsConfig(
                List.of(ElementPlacement.raw("Z", new Point(0,0))), List.of(), List.of(), List.of(), List.of()
            ), "explosion-markers-placing", "location not found in topology" },
            new Object[] { new DeckMapControlsConfig(
                List.of(), List.of(ElementPlacement.raw("Z", new Point(0,0))), List.of(), List.of(), List.of()
            ), "fire-buttons-placing", "location not found in topology" },
            new Object[] { new DeckMapControlsConfig(
                List.of(), List.of(), List.of(ElementPlacement.raw("Z", new Point(0,0))), List.of(), List.of()
            ), "evacuation-buttons-placing", "location not found in topology" },
            new Object[] { new DeckMapControlsConfig(
                List.of(), List.of(), List.of(), List.of(), List.of(new HydrantPlacement("Z", new Point(0,0), 1))
            ), "hydrant-placing", "location not found in topology" }
        );
    }

    @ParameterizedTest(name = "controls bad {1} reports {2}")
    @MethodSource
    void check_ControlsBadLocationCodeReportsFileAndSection(DeckMapControlsConfig badControls, String section, String expectedMsg) {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapGroupsConfig groups = makeEmptyGroups();
        DeckMapGeometryConfig geo = makeEmptyGeometry();

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, badControls, groups, geo));

        String msg = ex.getMessage();
        assertThat(msg, containsString("config/controls.yaml"));
        assertThat(msg, containsString(section));
        assertThat(msg, containsString(expectedMsg));
    }

    // --- Group B: door-buttons-placing ---
    @Test
    void check_DoorButtonsPlacingNonTwoLetterCodeReportsMustBeTwoLetter() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig bad = new DeckMapControlsConfig(
            List.of(), List.of(), List.of(), List.of(ElementPlacement.raw("Z", new Point(0,0))), List.of()
        );
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, bad, makeEmptyGroups(), makeEmptyGeometry()));
        assertThat(ex.getMessage(), containsString("must be 2-letter door code"));
    }

    @Test
    void check_DoorButtonsPlacingBadEndpointReportsEndpointNotFound() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig bad = new DeckMapControlsConfig(
            List.of(), List.of(), List.of(), List.of(ElementPlacement.raw("AZ", new Point(0,0))), List.of()
        );
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, bad, makeEmptyGroups(), makeEmptyGeometry()));
        assertThat(ex.getMessage(), containsString("endpoint location not found in topology"));
    }

    // --- Group C: groups locationCode, parameterized ---
    static Stream<Object[]> check_GroupsBadLocationCodeReports() {
        Point p = new Point(0,0);
        return Stream.of(
            new Object[] { "ventilation-group", groupsWithVentilation(new ToggleGroupConfig<>(List.of(new VentilationButtonSpec("Z", p)), null)) },
            new Object[] { "flammable-group", groupsWithFlammable(new ToggleGroupConfig<>(List.of(new FlammableButtonSpec("Z", p)), null)) },
            new Object[] { "machinery-damage-group", groupsWithMachineryDamage(new ToggleGroupConfig<>(List.of(new MachineryDamageButtonSpec("Z", p)), null)) },
            new Object[] { "explosion-group", groupsWithExplosion(new ToggleGroupConfig<>(List.of(new ExplosionButtonSpec("Z", p)), null)) },
            new Object[] { "frontline-balance-label-group", groupsWithFrontline(new HydrantsGroupConfig<>(List.of(new FrontlineBalanceGroupSpec("Z", p, HydrantLabelSize.FULL)), 10, 10, 10)) },
            new Object[] { "firefighting-step-label-group", groupsWithFirefighting(new HydrantsGroupConfig<>(List.of(new FirefightingStepGroupSpec("Z", p, HydrantLabelSize.FULL)), 10, 10, 10)) }
        );
    }

    @ParameterizedTest(name = "groups bad {0} reports location not found")
    @MethodSource
    void check_GroupsBadLocationCodeReports(String section, DeckMapGroupsConfig badGroups) {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, ctrl, badGroups, makeEmptyGeometry()));
        assertThat(ex.getMessage(), containsString("location not found in topology"));
    }

    // --- Group D: other groups codes ---
    @Test
    void check_FireSensorGroupBadSensorCodeReportsSensorNotFound() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        ToggleGroupConfig<FireSensorButtonSpec> bad = new ToggleGroupConfig<>(List.of(new FireSensorButtonSpec("Z", new Point(0,0))), null);
        DeckMapGroupsConfig groups = groupsWithSensor(bad);
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, ctrl, groups, makeEmptyGeometry()));
        assertThat(ex.getMessage(), containsString("sensor not found in topology"));
    }

    @Test
    void check_HydrantOutletLabelGroupBadTitleCodeReportsHydrantTitleNotFound() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        HydrOutletLabelGroupConfig bad = new HydrOutletLabelGroupConfig(List.of(new HydrLabelSpec("Z", new Point(0,0))), 10, 10, 10);
        DeckMapGroupsConfig groups = groupsWithHydrantLabel(bad);
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, ctrl, groups, makeEmptyGeometry()));
        assertThat(ex.getMessage(), containsString("hydrant title not found in topology"));
    }

    @Test
    void check_DoorButtonGroupBadCodeReportsDoorLinkCodeNotFound() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        DoorButtonGroupConfig bad = new DoorButtonGroupConfig(List.of(new DoorGlyphSpec("ZZ", null, null, null)), null);
        DeckMapGroupsConfig groups = groupsWithDoor(bad);
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, ctrl, groups, makeEmptyGeometry()));
        assertThat(ex.getMessage(), containsString("door/link code not found in topology"));
    }

    // --- Group E: geometry and aggregation, null safety ---
    @Test
    void check_GeometryUnknownLocationCodeInCoordsReportsNotInTopology() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        DeckMapGroupsConfig groups = makeEmptyGroups();
        DeckMapGeometryConfig badGeo = new DeckMapGeometryConfig(
            List.of(new LocationCoordinateSpec("Z", List.of())), List.of()
        );
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, ctrl, groups, badGeo));
        assertThat(ex.getMessage(), containsString("code not found in topology location-labels"));
    }

    @Test
    void check_GeometryBorderLinkMismatchReportsDoNotExactlyMatch() {
        DeckMapTopologyConfig baseTopo = minimalTopoWithCodes("A", "B");
        // add a border
        DeckMapTopologyConfig topoWithBorder = new DeckMapTopologyConfig(baseTopo.locationsLabels(), List.of(new BorderSpec("AB", 1.0)), List.of(), List.of(),
            new FireHoseSpansSpec(List.of(), List.of()), List.of(), List.of(), List.of());
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        DeckMapGroupsConfig groups = makeEmptyGroups();
        DeckMapGeometryConfig badGeo = new DeckMapGeometryConfig(List.of(), List.of()); // no borders
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topoWithBorder, ctrl, groups, badGeo));
        assertThat(ex.getMessage(), containsString("do not exactly match"));
    }

    @Test
    void check_GeometryUnknownBorderLinkReportsLinkNotFound() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B"); // no borders
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        DeckMapGroupsConfig groups = makeEmptyGroups();
        DeckMapGeometryConfig badGeo = new DeckMapGeometryConfig(
            List.of(), List.of(new BorderCoordinateSpec("ZZ", List.of()))
        );

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, ctrl, groups, badGeo));

        assertThat(ex.getMessage(), containsString("border-coordinates/link=ZZ"));
        assertThat(ex.getMessage(), containsString("link not found in topology borders"));
    }

    @Test
    void check_GeometryNonEmptyAllMatchForTankPasses() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        DeckMapGroupsConfig groups = makeEmptyGroups();
        DeckMapGeometryConfig goodGeo = new DeckMapGeometryConfig(
            List.of(new LocationCoordinateSpec("A", List.of()), new LocationCoordinateSpec("B", List.of())), List.of()
        );
        assertDoesNotThrow(() -> ConfigIntegrityChecker.check(topo, ctrl, groups, goodGeo));
    }

    @Test
    void check_AggregatesMultipleViolations() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig badCtrl = new DeckMapControlsConfig(
            List.of(ElementPlacement.raw("Z", new Point(0,0))), List.of(), List.of(), List.of(), List.of()
        );
        ToggleGroupConfig<VentilationButtonSpec> badVent = new ToggleGroupConfig<>(List.of(new VentilationButtonSpec("Z", new Point(0,0))), null);
        DeckMapGroupsConfig badGroups = groupsWithVentilation(badVent);
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> ConfigIntegrityChecker.check(topo, badCtrl, badGroups, makeEmptyGeometry()));
        assertThat(ex.getViolations(), hasSize(greaterThanOrEqualTo(2)));
        assertThat(ex.getMessage(), containsString("explosion-markers-placing"));
        assertThat(ex.getMessage(), containsString("ventilation-group"));
    }

    @Test
    void check_NullGeometryOrListsDoesNotThrowNPE() {
        DeckMapTopologyConfig topo = minimalTopoWithCodes("A", "B");
        DeckMapControlsConfig ctrl = new DeckMapControlsConfig(List.of(), List.of(), List.of(), List.of(), List.of());
        DeckMapGroupsConfig groups = makeEmptyGroups();
        // null geo
        assertDoesNotThrow(() -> ConfigIntegrityChecker.check(topo, ctrl, groups, null));
        assertDoesNotThrow(() -> ConfigIntegrityChecker.check(topo, ctrl, groups, new DeckMapGeometryConfig(null, null)));
    }

    // helpers for new tests
    private static DeckMapTopologyConfig minimalTopoWithCodes(String... codes) {
        List<LocationSpec> locs = new ArrayList<>();
        for (String c : codes)
            locs.add(new LocationSpec(c, null, 3, null, null, null, null, null, null));
        return new DeckMapTopologyConfig(locs, List.of(), List.of(), List.of(),
            new FireHoseSpansSpec(List.of(), List.of()), List.of(), List.of(), List.of());
    }

    private static DeckMapGroupsConfig groupsWithVentilation(ToggleGroupConfig<VentilationButtonSpec> group) {
        return new DeckMapGroupsConfig(group, emptyFlammable(), emptyMachineryDamage(), emptySensor(),
            emptyExplosion(), emptyFrontlineBalance(), emptyFirefightingStep(), emptyHydrantLabels(),
            emptyDoorButtons(), List.of(), List.of());
    }

    private static DeckMapGroupsConfig groupsWithFlammable(ToggleGroupConfig<FlammableButtonSpec> group) {
        return new DeckMapGroupsConfig(emptyVentilation(), group, emptyMachineryDamage(), emptySensor(),
            emptyExplosion(), emptyFrontlineBalance(), emptyFirefightingStep(), emptyHydrantLabels(),
            emptyDoorButtons(), List.of(), List.of());
    }

    private static DeckMapGroupsConfig groupsWithMachineryDamage(ToggleGroupConfig<MachineryDamageButtonSpec> group) {
        return new DeckMapGroupsConfig(emptyVentilation(), emptyFlammable(), group, emptySensor(),
            emptyExplosion(), emptyFrontlineBalance(), emptyFirefightingStep(), emptyHydrantLabels(),
            emptyDoorButtons(), List.of(), List.of());
    }

    private static DeckMapGroupsConfig groupsWithExplosion(ToggleGroupConfig<ExplosionButtonSpec> group) {
        return new DeckMapGroupsConfig(emptyVentilation(), emptyFlammable(), emptyMachineryDamage(), emptySensor(),
            group, emptyFrontlineBalance(), emptyFirefightingStep(), emptyHydrantLabels(),
            emptyDoorButtons(), List.of(), List.of());
    }

    private static DeckMapGroupsConfig groupsWithFrontline(HydrantsGroupConfig<FrontlineBalanceGroupSpec> group) {
        return new DeckMapGroupsConfig(emptyVentilation(), emptyFlammable(), emptyMachineryDamage(), emptySensor(),
            emptyExplosion(), group, emptyFirefightingStep(), emptyHydrantLabels(),
            emptyDoorButtons(), List.of(), List.of());
    }

    private static DeckMapGroupsConfig groupsWithFirefighting(HydrantsGroupConfig<FirefightingStepGroupSpec> group) {
        return new DeckMapGroupsConfig(emptyVentilation(), emptyFlammable(), emptyMachineryDamage(), emptySensor(),
            emptyExplosion(), emptyFrontlineBalance(), group, emptyHydrantLabels(),
            emptyDoorButtons(), List.of(), List.of());
    }

    private static ToggleGroupConfig<VentilationButtonSpec> emptyVentilation() { return new ToggleGroupConfig<>(List.of(), null); }
    private static ToggleGroupConfig<FlammableButtonSpec> emptyFlammable() { return new ToggleGroupConfig<>(List.of(), null); }
    private static ToggleGroupConfig<MachineryDamageButtonSpec> emptyMachineryDamage() { return new ToggleGroupConfig<>(List.of(), null); }
    private static ToggleGroupConfig<FireSensorButtonSpec> emptySensor() { return new ToggleGroupConfig<>(List.of(), null); }
    private static ToggleGroupConfig<ExplosionButtonSpec> emptyExplosion() { return new ToggleGroupConfig<>(List.of(), null); }
    private static HydrantsGroupConfig<FrontlineBalanceGroupSpec> emptyFrontlineBalance() { return new HydrantsGroupConfig<>(List.of(), 0,0,0); }
    private static HydrantsGroupConfig<FirefightingStepGroupSpec> emptyFirefightingStep() { return new HydrantsGroupConfig<>(List.of(), 0,0,0); }
    private static HydrOutletLabelGroupConfig emptyHydrantLabels() { return new HydrOutletLabelGroupConfig(List.of(), 0,0,0); }
    private static DoorButtonGroupConfig emptyDoorButtons() { return new DoorButtonGroupConfig(List.of(), null); }

    private static DeckMapGroupsConfig groupsWithSensor(ToggleGroupConfig<FireSensorButtonSpec> group) {
        return new DeckMapGroupsConfig(emptyVentilation(), emptyFlammable(), emptyMachineryDamage(), group,
            emptyExplosion(), emptyFrontlineBalance(), emptyFirefightingStep(), emptyHydrantLabels(),
            emptyDoorButtons(), List.of(), List.of());
    }

    private static DeckMapGroupsConfig groupsWithHydrantLabel(HydrOutletLabelGroupConfig group) {
        return new DeckMapGroupsConfig(emptyVentilation(), emptyFlammable(), emptyMachineryDamage(), emptySensor(),
            emptyExplosion(), emptyFrontlineBalance(), emptyFirefightingStep(), group,
            emptyDoorButtons(), List.of(), List.of());
    }

    private static DeckMapGroupsConfig groupsWithDoor(DoorButtonGroupConfig group) {
        return new DeckMapGroupsConfig(emptyVentilation(), emptyFlammable(), emptyMachineryDamage(), emptySensor(),
            emptyExplosion(), emptyFrontlineBalance(), emptyFirefightingStep(), emptyHydrantLabels(),
            group, List.of(), List.of());
    }
}
