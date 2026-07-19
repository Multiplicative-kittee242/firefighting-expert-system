package gui.solution;

import gui.Localization;
import domain.Explanation;
import domain.types.CompartmentType;
import domain.Extinguisher;
import domain.types.ExtinguisherType;
import domain.Link;
import domain.Location;
import domain.registry.LocationRegistry;
import domain.registry.TopologyModel;
import org.junit.jupiter.api.Test;
import fixtures.FakeClipsReadOnlyService;
import fixtures.TestLocations;

import javax.swing.JTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Regression tests for {@link SolutionResultsController} actions-table (re)building and
 * special-case message formatting (e.g. cross-compartment firefighting naming).
 * Uses {@link fixtures.FakeClipsReadOnlyService} as a configurable fake collaborator
 * to drive the controller's CLIPS-dependent paths without a real backend.
 */
class SolutionResultsControllerTest {

    private static final TopologyModel TOPOLOGY = TopologyModel.from(
        TopologyModel.RawTopology.empty().withLocations(TestLocations.identities("A", "B", "D", "E", "F")));

    private static final Set<Location> EVACUATE = setOf("A", "B", "D");
    private static final Set<Location> VENTILATION_OFF = setOf("E", "F");
    private static final int PRIORITY_ROW_COUNT = EVACUATE.size() + VENTILATION_OFF.size();

    // Doors for the sealing-row-boundary tests. Constructed directly (the controller reads a door's
    // own code/from/to and never resolves it through the topology, so these need no registry entry —
    // which is also why TOPOLOGY above is built with an empty link list). DOOR_BA_REVERSED carries a
    // deliberately non-ascending code ("BA": 'B' is not < 'A") to exercise the direction filter.
    private static final Link DOOR_AB = new Link("AB", new Location("A"), new Location("B"));
    private static final Link DOOR_DE = new Link("DE", new Location("D"), new Location("E"));
    private static final Link DOOR_BA_REVERSED = new Link("BA", new Location("B"), new Location("A"));

    @Test
    void onPhaseChanged_RebuildingTheSamePhaseDoesNotDuplicateRows() {
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withEvacuation(EVACUATE)
            .withVentilationOff(VENTILATION_OFF);
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.PRIORITY_MEASURES);
        assertThat("first fill from an empty table",
            table.getRowCount(), is(PRIORITY_ROW_COUNT));

        // A door/vent click re-fires the same phase; the table must be rebuilt, not appended to.
        controller.onPhaseChanged(SolutionTreeSection.PRIORITY_MEASURES);
        assertThat("rebuild of an already-populated phase must not duplicate rows",
            table.getRowCount(), is(PRIORITY_ROW_COUNT));

        // And repeatedly (odd counts previously left different leftovers each time).
        controller.onPhaseChanged(SolutionTreeSection.PRIORITY_MEASURES);
        assertThat("repeated rebuild of an already-populated phase must not duplicate rows",
            table.getRowCount(), is(PRIORITY_ROW_COUNT));
    }

    @Test
    void onPhaseChanged_ClearsTheTableCompletelyWhenSwitchingToRoot() {
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withEvacuation(EVACUATE)
            .withVentilationOff(VENTILATION_OFF);
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.PRIORITY_MEASURES);
        assertThat("priority measures populates expected number of rows from fixtures",
            table.getRowCount(), is(PRIORITY_ROW_COUNT));

        controller.onPhaseChanged(SolutionTreeSection.ROOT); // ROOT adds nothing
        assertThat("ROOT must fully clear the table",
            table.getRowCount(), is(0));
    }

    @Test
    void onPhaseChanged_PriorityMeasuresIncludesExtinguisherRowsPerFireLocation() {
        Location fireRoom = new Location("A");
        Extinguisher co2 = new Extinguisher("est_a", fireRoom, ExtinguisherType.CARBON_DIOXIDE);
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withFireLocations(Set.of(fireRoom))
            .withExtinguishers(Map.of(fireRoom, List.of(co2)));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.PRIORITY_MEASURES);

        assertThat("one extinguisher row for the single fire location", table.getRowCount(), is(1));
    }

    @Test
    void onPhaseChanged_FirefightingRowNamesTheComputedFromLocation() {
        Location fireRoom = new Location("D");
        Location fromLocation = new Location("A");
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withFireLocations(Set.of(fireRoom))
            .withFirefightingPlan(Collections.singletonMap(fireRoom, fromLocation));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.FIREFIGHTING);

        String expectedMessage = String.format(Localization.get("message.firefighting"), "A");
        assertThat("firefighting phase produces exactly one row", table.getRowCount(), is(1));
        assertThat("row message uses the resolved from-location code", table.getValueAt(0, 3), is(expectedMessage));
    }

    /**
     * Regression test for the fire-at-P bug: CLIPS computes no door-to-door route to a fire room
     * whose only reachable neighbor is in a different tank compartment (the {@code R}/{@code T}
     * pattern — a cross-compartment door with no matching border, see
     * {@code clips.ClipsEngineAccess#getStepFrom}'s javadoc). Rather than a blank "from"
     * (previously {@code String.format(..., "")}), the row must name that neighbor's tank number.
     */
    @Test
    void onPhaseChanged_FirefightingRowNamesTheCrossCompartmentNeighborsTankWhenNoRouteIsComputed() {
        final Location fireRoom = new Location("P");
        final int crossCompartmentTank = 2;
        final LocationRegistry.RawLocation crossCompartmentNeighbor = new LocationRegistry.RawLocation(
            "T", Location.NO_AREA, crossCompartmentTank, CompartmentType.UNINHABITED, null, null, null, false, false);

        // Minimal topology with a cross-compartment link PT (P and T in different tanks).
        final List<LocationRegistry.RawLocation> locations = List.of(LocationRegistry.RawLocation.identity("P"), crossCompartmentNeighbor);
        final TopologyModel topology = TopologyModel.from(
            TopologyModel.RawTopology.empty().withLocations(locations).withLinkCodes(List.of("PT")));

        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withFireLocations(Set.of(fireRoom))
            .withFirefightingPlan(Collections.singletonMap(fireRoom, null));
        SolutionResultsController controller = new SolutionResultsController(clips, topology);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.FIREFIGHTING);

        String expectedMessage = String.format(Localization.get("message.firefighting.no.access"),
            "P", crossCompartmentTank);
        assertThat("firefighting phase for unreachable cross-compartment fire produces one row",
            table.getRowCount(), is(1));
        assertThat("message names the neighbor tank rather than leaving from-location blank",
            table.getValueAt(0, 3), is(expectedMessage));
    }

    // ==================== door-sealing row boundary logic ====================
    // addSealingDoorCloseRows / addSealingDoorKeepOpenRows carry two subtleties the real-click
    // integration test (SolutionResultsIntegrationTest) does not reach — there inclusive is always
    // true (SEALING) and keep-open is always empty. These four unit tests pin them directly:
    //   (1) the trailing-element drop: limit = size - (inclusive ? 0 : 1) for close rows, and an
    //       unconditional size - 1 for keep-open rows;
    //   (2) the direction filter: only doors whose code is ascending (charAt(0) < charAt(1)) yield
    //       a row, deduplicating a door reported in both orientations down to its canonical one.

    @Test
    void onPhaseChanged_PriorityMeasuresDropsTheTrailingToCloseDoor() {
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withSealingDoorsToClose(List.of(DOOR_AB, DOOR_DE));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.PRIORITY_MEASURES);

        // inclusive=false → limit = size - 1 → only the first door survives; the trailing DE is dropped.
        assertThat(table.getRowCount(), is(1));
        assertThat(roomColumnValues(table), contains("A, B"));
    }

    @Test
    void onPhaseChanged_SealingKeepsEveryToCloseDoor() {
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withSealingDoorsToClose(List.of(DOOR_AB, DOOR_DE));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.SEALING);

        // Same two doors as the PRIORITY_MEASURES test above, but inclusive=true keeps the trailing one.
        assertThat(table.getRowCount(), is(2));
        assertThat(roomColumnValues(table), containsInAnyOrder("A, B", "D, E"));
    }

    @Test
    void onPhaseChanged_SealingDropsTheTrailingKeepOpenDoor() {
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withSealingDoorsKeepOpen(List.of(DOOR_AB, DOOR_DE));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.SEALING);

        // keep-open always drops its trailing element (limit = size - 1), and the surviving row
        // carries the "lay out a fire hose" wording — distinct from a plain to-close row.
        String expectedMessage = String.format(Localization.get("message.sealing.door.close.with.hose"), "A", "B");
        assertThat(table.getRowCount(), is(1));
        assertThat(roomColumnValues(table), contains("A, B"));
        assertThat(table.getValueAt(0, 3), is(expectedMessage));
    }

    @Test
    void onPhaseChanged_SealingSkipsDoorsWhoseCodeIsNotAscending() {
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withSealingDoorsToClose(List.of(DOOR_BA_REVERSED, DOOR_DE));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);
        JTable table = controller.getActionsTable();

        controller.onPhaseChanged(SolutionTreeSection.SEALING);

        // inclusive=true so nothing is dropped for length; the reversed-code BA is removed by the
        // direction filter (it is the FIRST element, so a trailing-drop cannot explain its absence),
        // leaving only DE.
        assertThat(table.getRowCount(), is(1));
        assertThat(roomColumnValues(table), contains("D, E"));
    }

    // ==================== explanation-popup routing ====================
    // resolveExplanationForRow (the routing resolveExplanationFromClips/resolveSealingExplanation
    // perform, exercised without ActionsTableMouseAdapter's popup-display side effect — JPopupMenu#show
    // requires the table to be showing on screen and throws IllegalComponentStateException headless,
    // confirmed empirically) was previously untested: FakeClipsReadOnlyService hardcoded every
    // getExplanationFor* method to Explanation.EMPTY, so a routing bug (the wrong phase resolving to
    // the wrong ClipsReadOnlyService method) would have been invisible — both the right and the wrong
    // call would have returned the same empty value. The fake now returns a fixed, distinguishable
    // literal per method instead, so these tests can tell which one a given row actually reached.

    @Test
    void resolveExplanationForRow_RoutesEvacuationRowToEvacuationExplanation() {
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withEvacuation(Set.of(new Location("A")));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);

        controller.onPhaseChanged(SolutionTreeSection.EVACUATION);

        assertThat(controller.resolveExplanationForRow(0),
            is(new Explanation("evacuation-antec1", "evacuation-antec2", "evacuation-consec")));
    }

    @Test
    void resolveExplanationForRow_RoutesSealingVentilationRoomRowToLocationExplanation() {
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withVentilationOff(Set.of(new Location("A")));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);

        controller.onPhaseChanged(SolutionTreeSection.SEALING);

        // A single-character room code (not a door's four-character "A, B") routes to the
        // room/location explanation, not the door-sealing one — see resolveSealingExplanation.
        assertThat(controller.resolveExplanationForRow(0),
            is(new Explanation("location-antec1", "location-antec2", "location-consec")));
    }

    @Test
    void resolveExplanationForRow_RoutesSealingDoorRowToDoorSealingExplanation() {
        // Unlike TOPOLOGY (built with no links, for the door-boundary tests above, which never
        // resolve a door through it), DoorLabel.fromDisplayString parses the row's "A, B" text back
        // into a Link via topology.link(...), so this needs an actual registered "AB" link.
        TopologyModel topologyWithDoorLink = TopologyModel.from(
            TopologyModel.RawTopology.empty()
                .withLocations(TestLocations.identities("A", "B"))
                .withLinkCodes(List.of("AB")));
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withSealingDoorsToClose(List.of(DOOR_AB));
        SolutionResultsController controller = new SolutionResultsController(clips, topologyWithDoorLink);

        controller.onPhaseChanged(SolutionTreeSection.SEALING);

        // The four-character "A, B" room code routes to the door-sealing explanation, not the
        // single-room one — see resolveSealingExplanation.
        assertThat(controller.resolveExplanationForRow(0),
            is(new Explanation("door-sealing-antec1", "door-sealing-antec2", "door-sealing-consec")));
    }

    @Test
    void resolveExplanationForRow_RoutesFlammableMachineryAndExplosionRowsToTheirOwnExplanations() {
        // All three share one SolutionTreeSection.PREVENTION phase-tree call (see onPhaseChanged's
        // PREVENTION case), added in this fixed order — flammable, then machinery, then explosion —
        // so a single onPhaseChanged call produces exactly one row per SolutionPhase, at these indices.
        Location room = new Location("A");
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withIsolationPhase(Set.of(room))
            .withMachineryDamage(Set.of(room))
            .withExplosionPhase(Set.of(room));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);

        controller.onPhaseChanged(SolutionTreeSection.PREVENTION);

        assertThat("row 0: flammable-prevention row routes to the flammable explanation",
            controller.resolveExplanationForRow(0),
            is(new Explanation("flammable-antec1", "flammable-antec2", "flammable-consec")));
        assertThat("row 1: machinery-damage row routes to the machinery-damage explanation",
            controller.resolveExplanationForRow(1),
            is(new Explanation("machinery-damage-antec1", "machinery-damage-antec2", "machinery-damage-consec")));
        assertThat("row 2: explosion-prevention row routes to the explosions explanation",
            controller.resolveExplanationForRow(2),
            is(new Explanation("explosions-antec1", "explosions-antec2", "explosions-consec")));
    }

    @Test
    void resolveExplanationForRow_PhaseWithoutExplanationSupportReturnsEmpty() {
        Location fireRoom = new Location("D");
        Location fromLocation = new Location("A");
        FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
            .withFireLocations(Set.of(fireRoom))
            .withFirefightingPlan(Collections.singletonMap(fireRoom, fromLocation));
        SolutionResultsController controller = new SolutionResultsController(clips, TOPOLOGY);

        controller.onPhaseChanged(SolutionTreeSection.FIREFIGHTING);

        // FIREFIGHTING rows carry SolutionPhase.FIREFIGHTING, which resolveExplanationFromClips's
        // switch has no case for — falls through to its `default -> Explanation.EMPTY` branch.
        assertThat(controller.resolveExplanationForRow(0).isEmpty(), is(true));
    }

    private static List<String> roomColumnValues(JTable table) {
        List<String> rooms = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++)
            rooms.add((String) table.getValueAt(row, 2));
        return rooms;
    }

    private static Set<Location> setOf(String... codes) {
        Set<Location> locations = new LinkedHashSet<>();
        for (String code : codes)
            locations.add(new Location(code));
        return locations;
    }
}
