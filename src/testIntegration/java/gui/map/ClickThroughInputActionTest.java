package gui.map;

import clips.ClipsReportService;
import clips.FireIncidentSnapshot;
import clips.values.DoorState;
import clips.values.EvacuationStatus;
import clips.values.MachineryDamageAction;
import clips.values.VentilationAction;
import clips.values.internal.ExplosionClipsAction;
import clips.values.internal.FlammablePreventionClipsAction;
import config.groups.GroupKey;
import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import config.specification.GroupLayerSpec;
import domain.Location;
import domain.registry.TopologyModel;
import gui.Localization;
import gui.actions.ActionDispatcher;
import gui.actions.ClipsValuesMapper;
import gui.actions.InputActionListener;
import gui.solution.SolutionPhaseTree;
import gui.solution.SolutionResultsController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import util.ResourceUtil;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Click-through tests: every real button in a real (thematically-trimmed) {@link DeckMapController}
 * group, clicked for real via {@code doClick()} — through the real {@code gui.actions.InputAction}/
 * {@link ActionDispatcher} wiring (via the shared {@link InputActionListener}, the same class
 * {@code app.Main} uses) — down to a mocked {@link ClipsReportService}. Unlike
 * {@code gui.actions.InputActionDispatchTest} (which hand-builds an {@code InputAction} and calls
 * {@code apply} directly), this proves every button in the group is wired to the *correct* action
 * with the *correct* identity — the one thing a purely synthetic {@code apply()} call cannot catch.
 * Looping over {@code forEachControl} (rather than picking one button via {@code getControlFor})
 * also catches a wrong-location mix-up that a single hand-picked button would miss.
 * <p>
 * Covers every {@code collect*Changes}-driven group — Evacuation, Ventilation, DoorSealing,
 * Explosion, Flammable, MachineryDamage — plus Fire/FireSensor (which route through
 * {@code reportFireIncident} directly instead) and a regression guard documenting that
 * Extinguisher has no configured buttons yet.
 * <p>
 * Lives in package {@code gui.map} to call {@link DeckMapController}'s package-private
 * {@code @VisibleForTesting} group getters (see its class-level comment) — the only way to click
 * the exact button instances the controller will later query via {@code collect*Changes}.
 * <p>
 * Clicks are dispatched via {@link #clickOnEdt}, not a bare {@code button.doClick()}: Swing is
 * single-threaded by contract, and driving it from a plain JUnit worker thread is unsupported —
 * empirically it produced rare, load-dependent {@code WantedButNotInvoked} failures (reproducible
 * only when this suite ran alongside other test classes, never in isolation — a signature of an
 * off-EDT race, not a wiring bug). Routing every click through the real EDT via
 * {@code SwingUtilities.invokeAndWait} removed the flakiness entirely.
 * <p>
 * Per-button checks are collected as {@link Executable}s and run through {@link
 * org.junit.jupiter.api.Assertions#assertAll(String, java.util.stream.Stream)} rather than
 * asserted inline in the loop: a bad button among many should not hide the state of the rest —
 * {@code assertAll} runs every check regardless of earlier failures and reports all of them
 * together in one combined failure.
 * <p>
 * The final click-count assertion in each test checks an exact number, read back from the same
 * config section that drives the group's construction (e.g. {@code controlConfig.fireButtons()
 * .size()}) — not a bare {@code greaterThan(0)}. A bare positivity check would not notice the loop
 * silently skipping some of the group's buttons, nor would it notice the constructor silently
 * dropping or duplicating entries from config; comparing against the real config size catches both.
 */
@ExtendWith(MockitoExtension.class)
class ClickThroughInputActionTest {

    @Mock
    private ClipsReportService clipsReportService;
    @Mock
    private SolutionPhaseTree solutionTree;
    @Mock
    private SolutionResultsController resultsController;

    /**
     * {@code FireActionInput} doesn't go through {@code collect*Changes}: it calls
     * {@code reportFireIncident} directly, whose return value {@code representFire} immediately
     * reads ({@code state.explosionThreatLocations()}, etc.) — an unstubbed mock would answer
     * {@code null} for a plain domain type and NPE there, unlike the collection-returning
     * {@code reportExplosionPreventionChanges} above. Stubbed with a mocked
     * {@link FireIncidentSnapshot}: its own accessors all return {@code Set}/{@code List}/
     * {@code Map}, which Mockito's default answer fills with empty collections, so no further
     * stubbing cascades. One button per location here (unlike FireSensor below), so the same
     * times(1)-per-click shape as the other groups is safe. No "revert" meaning for a second
     * click (reporting a fire is a one-shot action) — but {@code FireButton} extends {@code
     * AbstractStickyFireButton}, which disables itself shortly after being selected (scheduled via
     * its own {@code invokeLater}, drained by {@link #clickOnEdt}). That's an assumption worth
     * locking down too: this test clicks each button twice and asserts the second click produced
     * no additional report.
     */
    @Test
    void clickingEachFireButton_DispatchesReportFireIncidentAndUpdatesEventsForItsOwnLocationThenIgnoresSecondClick() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.FIRE_BUTTON_GROUP, listener);
        when(clipsReportService.reportFireIncident(any())).thenReturn(mock(FireIncidentSnapshot.class));
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        AtomicInteger clickedCount = new AtomicInteger();
        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getFireButtonGroup().forEachControl((location, button) -> checks.add(() -> {
            clickOnEdt(button);
            clickedCount.incrementAndGet();
            verify(clipsReportService).reportFireIncident(location);
            verify(resultsController).updateEvents(location);
            assertThat(button.isEnabled(), is(false));

            clickOnEdt(button);
            verify(clipsReportService, times(1)).reportFireIncident(location);
            verify(resultsController, times(1)).updateEvents(location);
        }));
        assertAll("fire button clicks", checks.stream());

        int expectedButtonCount = setup.config().getControlConfig().fireButtons().size();
        assertThat(clickedCount.get(), is(expectedButtonCount));
        verify(solutionTree, times(clickedCount.get())).resetPhaseAndNotify();
        verify(solutionTree, never()).refreshCurrentPhase();
    }

    /**
     * Unlike every other group, {@code FireSensorButtonGroup}'s domain key is {@link
     * domain.FireSensor}, not {@link Location} — and several sensors share the same location
     * (e.g. D1..D6 all resolve to D, J1..J8 all resolve to J, per {@code groups.yaml}'s
     * fire-sensor-group). A plain times(1)-per-click check (as used for the other groups, where
     * one button maps to exactly one location) would wrongly fail the second time a shared
     * location's sensor is clicked — Mockito's {@code verify(mock).method(x)} checks for exactly
     * one matching invocation across the mock's *entire* history, not just since the last check.
     * Tracked per-location cumulative counts instead. Same sticky self-disable as the plain fire
     * button above ({@code FireSensorButton} also extends {@code AbstractStickyFireButton}) — each
     * sensor is clicked twice, asserting the second click adds nothing beyond the first.
     */
    @Test
    void clickingEachFireSensorButton_DispatchesReportFireIncidentForItsOwnSensorLocationThenIgnoresSecondClick() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.FIRE_SENSOR_GROUP, listener);
        when(clipsReportService.reportFireIncident(any())).thenReturn(mock(FireIncidentSnapshot.class));
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        Map<Location, Integer> clicksSoFarByLocation = new HashMap<>();
        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getFireSensorGroup().forEachControl((sensor, button) -> checks.add(() -> {
            clickOnEdt(button);
            Location location = sensor.getLocation();
            int clicksSoFar = clicksSoFarByLocation.merge(location, 1, Integer::sum);
            verify(clipsReportService, times(clicksSoFar)).reportFireIncident(location);
            verify(resultsController, times(clicksSoFar)).updateEvents(location);
            assertThat(button.isEnabled(), is(false));

            clickOnEdt(button);
            verify(clipsReportService, times(clicksSoFar)).reportFireIncident(location);
            verify(resultsController, times(clicksSoFar)).updateEvents(location);
        }));
        assertAll("fire sensor button clicks", checks.stream());

        // Summed per-location counts equal the total number of sensors clicked (one increment per
        // sensor, on its first click only) — not the number of distinct locations, since several
        // sensors share a location.
        int totalClicks = clicksSoFarByLocation.values().stream().mapToInt(Integer::intValue).sum();
        int expectedSensorCount = setup.config().getGroupsConfig().fireSensorGroupConfig().items().size();
        assertThat(totalClicks, is(expectedSensorCount));
        verify(solutionTree, times(totalClicks)).resetPhaseAndNotify();
        verify(solutionTree, never()).refreshCurrentPhase();
    }

    /**
     * There is no "revert" here — {@code EvacuationButtonGroup#collectChanges} disables the button
     * once it reports {@code DONE} (see its source), so a second click cannot mean "undo" for this
     * group. That's an assumption, not just a comment: this test clicks each button *twice* and
     * asserts the button is disabled and the second click produced no additional report — if this
     * group ever stopped disabling the button, this would fail instead of silently going stale.
     */
    @Test
    void clickingEachEvacuationButton_DispatchesReportEvacuationChangesForItsOwnLocationThenIgnoresSecondClick() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.EVACUATION_GROUP, listener);
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        AtomicInteger clickedCount = new AtomicInteger();
        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getEvacuationGroup().forEachControl((location, button) -> checks.add(() -> {
            clickOnEdt(button);
            clickedCount.incrementAndGet();
            verify(clipsReportService).reportEvacuationChanges(Map.of(location, EvacuationStatus.DONE));
            assertThat(button.isEnabled(), is(false));

            clickOnEdt(button);
            verify(clipsReportService, times(1)).reportEvacuationChanges(Map.of(location, EvacuationStatus.DONE));
        }));
        assertAll("evacuation button clicks", checks.stream());

        int expectedButtonCount = setup.config().getControlConfig().evacuationButtons().size();
        assertThat(clickedCount.get(), is(expectedButtonCount));
        verify(solutionTree, times(clickedCount.get())).refreshCurrentPhase();
    }

    /**
     * Clicks each door twice: the first click reports {@code CLOSE}, the second (revert) reports
     * {@code OPEN} — unlike Evacuation, {@code DoorSealingButtonGroup} never disables its buttons,
     * so un-sealing a door is a real operator action worth covering.
     */
    @Test
    void clickingEachDoorSealingButton_DispatchesReportDoorSealingChangesForItsOwnDoorAndReverts() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.DOOR_SEALING_GROUP, listener);
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        AtomicInteger totalClicks = new AtomicInteger();
        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getDoorSealingGroup().forEachControl((door, button) -> checks.add(() -> {
            clickOnEdt(button);
            totalClicks.incrementAndGet();
            verify(clipsReportService).reportDoorSealingChanges(Map.of(door, DoorState.CLOSE));

            clickOnEdt(button);
            totalClicks.incrementAndGet();
            verify(clipsReportService).reportDoorSealingChanges(Map.of(door, DoorState.OPEN));
        }));
        assertAll("door sealing button clicks", checks.stream());

        int expectedDoorCount = setup.config().getControlConfig().doorButtons().size();
        assertThat(totalClicks.get(), is(2 * expectedDoorCount));
        verify(solutionTree, times(totalClicks.get())).refreshCurrentPhase();
    }

    /**
     * The "typed" group case: each flammable button carries a fixed material-specific type (from
     * its location's burning material — see {@code groups.yaml}'s flammable-group). Selecting it
     * reports {@code DONE}, remapped by the real {@code ClipsValuesMapper} regardless of type; a
     * second click (revert) reports the button's *own* type remapped instead (e.g. {@code OIL} →
     * {@code PUMP_OUT}, {@code CLOTHES} → {@code CARRY_OUT}) — proving the type round-trips
     * correctly, not just that everything converges on {@code DONE}.
     */
    @Test
    void clickingEachFlammableButton_DispatchesRemappedReportFlammablePreventionChangesAndReverts() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.FLAMMABLE_GROUP, listener);
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        AtomicInteger totalClicks = new AtomicInteger();
        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getFlammableGroup().forEachControl((location, button) -> checks.add(() -> {
            clickOnEdt(button);
            totalClicks.incrementAndGet();
            verify(clipsReportService).reportFlammablePreventionChanges(Map.of(location, FlammablePreventionClipsAction.DONE));

            clickOnEdt(button);
            totalClicks.incrementAndGet();
            FlammablePreventionClipsAction reverted = ClipsValuesMapper.toClips(button.getType());
            verify(clipsReportService).reportFlammablePreventionChanges(Map.of(location, reverted));
        }));
        assertAll("flammable button clicks", checks.stream());

        int expectedButtonCount = setup.config().getGroupsConfig().flammableGroupConfig().items().size();
        assertThat(totalClicks.get(), is(2 * expectedButtonCount));
        verify(solutionTree, times(totalClicks.get())).refreshCurrentPhase();
    }

    /**
     * Clicks each vent twice: the first click reports {@code OFF}, the second (revert) reports
     * {@code ON} — {@code VentilationButtonGroup} never disables its buttons.
     */
    @Test
    void clickingEachVentilationButton_DispatchesReportVentilationChangesForItsOwnLocationAndReverts() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.VENTILATION_GROUP, listener);
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        AtomicInteger totalClicks = new AtomicInteger();
        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getVentilationGroup().forEachControl((location, button) -> checks.add(() -> {
            clickOnEdt(button);
            totalClicks.incrementAndGet();
            verify(clipsReportService).reportVentilationChanges(Map.of(location, VentilationAction.OFF));

            clickOnEdt(button);
            totalClicks.incrementAndGet();
            verify(clipsReportService).reportVentilationChanges(Map.of(location, VentilationAction.ON));
        }));
        assertAll("ventilation button clicks", checks.stream());

        int expectedButtonCount = setup.config().getGroupsConfig().ventilationGroupConfig().items().size();
        assertThat(totalClicks.get(), is(2 * expectedButtonCount));
        verify(solutionTree, times(totalClicks.get())).refreshCurrentPhase();
    }

    /**
     * Clicks each button twice: the first click reports {@code DONE}, the second (revert) reports
     * {@code STOP} — {@code MachineryDamageButtonGroup} never disables its buttons.
     */
    @Test
    void clickingEachMachineryDamageButton_DispatchesReportMachineryDamagePreventionChangesForItsOwnLocationAndReverts() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.MACHINERY_DAMAGE_GROUP, listener);
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        AtomicInteger totalClicks = new AtomicInteger();
        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getMachineryDamageGroup().forEachControl((location, button) -> checks.add(() -> {
            clickOnEdt(button);
            totalClicks.incrementAndGet();
            verify(clipsReportService).reportMachineryDamagePreventionChanges(Map.of(location, MachineryDamageAction.DONE));

            clickOnEdt(button);
            totalClicks.incrementAndGet();
            verify(clipsReportService).reportMachineryDamagePreventionChanges(Map.of(location, MachineryDamageAction.STOP));
        }));
        assertAll("machinery damage button clicks", checks.stream());

        int expectedButtonCount = setup.config().getGroupsConfig().machineryDamageGroupConfig().items().size();
        assertThat(totalClicks.get(), is(2 * expectedButtonCount));
        verify(solutionTree, times(totalClicks.get())).refreshCurrentPhase();
    }

    /**
     * Same "typed group" pattern as flammable (see its javadoc above): each explosion button
     * carries a fixed material-specific {@code ExplosiveType}. Selecting it reports {@code DONE},
     * remapped regardless of type; the revert click reports the button's own type remapped instead
     * (e.g. {@code AIR} → {@code CARRY_OUT}, {@code OIL} → {@code PUMP_OUT}, {@code REAGENT} →
     * {@code TO_FIGHT}). {@code reportExplosionPreventionChanges} is the one report method with a
     * non-void return ({@code Set<Location>}); left unstubbed, Mockito's default answer returns an
     * empty set rather than {@code null} for collection-typed returns, so
     * {@code repaintExplosionLocations(Set.of())} runs safely with no extra stubbing needed.
     */
    @Test
    void clickingEachExplosionButton_DispatchesRemappedReportExplosionPreventionChangesAndReverts() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.EXPLOSION_GROUP, listener);
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        AtomicInteger totalClicks = new AtomicInteger();
        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getExplosionGroup().forEachControl((location, button) -> checks.add(() -> {
            clickOnEdt(button);
            totalClicks.incrementAndGet();
            verify(clipsReportService).reportExplosionPreventionChanges(Map.of(location, ExplosionClipsAction.DONE));

            clickOnEdt(button);
            totalClicks.incrementAndGet();
            ExplosionClipsAction reverted = ClipsValuesMapper.toClips(button.getType());
            verify(clipsReportService).reportExplosionPreventionChanges(Map.of(location, reverted));
        }));
        assertAll("explosion button clicks", checks.stream());

        int expectedButtonCount = setup.config().getGroupsConfig().explosionGroupConfig().items().size();
        assertThat(totalClicks.get(), is(2 * expectedButtonCount));
        verify(solutionTree, times(totalClicks.get())).refreshCurrentPhase();
    }

    /**
     * Locks down the side channel the test above does not touch: {@code
     * ExplosionButtonGroup#collectChanges} also pushes {@code collectPreventedLocations()} into
     * {@code FireIncidentState} on every call (see its own source), independent of the report-call
     * return value — this is what a real {@code MapPainter} ultimately reads (via {@code
     * fetchPendingExplosionPreventionLocations}) to decide which explosion markers are still
     * outstanding. Checked here through {@code DeckMapController#getPreventedExplosionLocations}
     * directly, deliberately not through the combined "pending" value: {@code
     * reportExplosionPreventionChanges} is left unstubbed (Mockito's default empty-set answer, same
     * as the test above), which drives {@code explosionThreatLocations} to empty after the first
     * click — fine here, since this test never reads that side, only the button-driven prevented
     * set, keeping the two independent tracks decoupled instead of fighting each other (an earlier
     * version of this test stubbed a constant "remaining" set instead, which fed back through {@code
     * repaintExplosionLocations} → {@code onExplosionDataChanged} → {@code setVisibleFor}, forcing
     * every button back to unselected right after its own click and turning the revert click into a
     * second select).
     * <p>
     * Today every location in this group has exactly one button (see {@code
     * ExplosionButtonGroup#isPrevented}'s {@code allMatch} over what is structurally always a
     * singleton list — {@code AbstractControlGroup} rejects a duplicate location key at
     * construction, and {@link domain.Location#getExplosiveMaterial()} is single-valued), so a
     * multi-button-per-location "only when every hazard is resolved" case cannot be exercised here.
     */
    @Test
    void clickingEachExplosionButton_UpdatesPreventedExplosionLocations() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.EXPLOSION_GROUP, listener);
        listener.setDispatcher(new ActionDispatcher(clipsReportService, setup.controller(), solutionTree, resultsController));

        List<Executable> checks = new ArrayList<>();
        setup.controller().inputGroups().getExplosionGroup().forEachControl((location, button) -> checks.add(() -> {
            assertThat(setup.controller().getPreventedExplosionLocations(), not(hasItem(location)));

            clickOnEdt(button);
            assertThat(setup.controller().getPreventedExplosionLocations(), hasItem(location));

            clickOnEdt(button);
            assertThat(setup.controller().getPreventedExplosionLocations(), not(hasItem(location)));
        }));
        assertAll("explosion prevented-location tracking", checks.stream());
    }

    /**
     * Regression guard, not a click-through test: {@code ExtinguisherButtonGroup} is always
     * constructed with zero elements — no {@code topology.yaml}/{@code groups.yaml} placement data
     * exists yet for extinguishers (see {@link DeckMapController}'s constructor comment). This
     * already locks down the "nothing to click, nothing can happen" assumption as strongly as
     * possible — with zero buttons there is no first click either, so a separate "second click
     * does nothing" test would add nothing here. If this ever starts failing, real placements were
     * added — replace it with a real click-through test shaped like the others above. The expected
     * count here is a hardcoded {@code 0}, not read from config, because nothing in config drives
     * it: {@code DeckMapController} always passes a hardcoded empty list, by design.
     */
    @Test
    void extinguisherGroup_HasNoConfiguredButtonsYet() {
        InputActionListener listener = new InputActionListener();
        ThematicSetup setup = buildThematicController(GroupKey.EXTINGUISHERS_GROUP, listener);

        AtomicInteger clickedCount = new AtomicInteger();
        setup.controller().inputGroups().getExtinguisherGroup().forEachControl((extinguisher, button) -> clickedCount.incrementAndGet());

        assertThat(clickedCount.get(), is(0));
    }

    /**
     * Recipe: keep the real hydrant-button-group specs (those groups stay unconditional — see
     * {@link DeckMapController}) but trim {@code group-layers} down to the one group under test,
     * with {@code initialVisibility = true} — required for {@code doClick()} to actually fire the
     * listener, since {@code AbstractControlGroup#addToMap} only enables a control when its
     * layer's initial visibility is true. Returns {@code config} alongside the controller so each
     * test can read back the exact expected button count from the same config section that drove
     * the group's construction, instead of a bare "at least one" sanity check.
     */
    private static ThematicSetup buildThematicController(GroupKey key, ActionListener listener) {
        DeckMapConfig deckMapConfig = DeckMapConfig.createDefault();
        TopologyModel topology = deckMapConfig.getTopologyConfig().buildTopologyModel();

        DeckMapAssemblyConfig realAssembly = DeckMapAssemblyConfig.createDefault(
            deckMapConfig.getGroupsConfig(), topology.allHydrantOutlets());
        DeckMapAssemblyConfig thematicAssembly = new DeckMapAssemblyConfig(
            List.of(new GroupLayerSpec(key, true)), realAssembly.hydrantButtonGroupSpecList());

        ImageIcon mapImage = new ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()));
        DeckMapController controller = new DeckMapController(thematicAssembly, deckMapConfig, topology, mapImage, listener);
        return new ThematicSetup(controller, deckMapConfig);
    }

    private record ThematicSetup(DeckMapController controller, DeckMapConfig config) {}

    /**
     * Runs {@code button.doClick()} on the AWT Event Dispatch Thread and blocks until it (and the
     * whole synchronous InputAction/ActionDispatcher/report chain it triggers) has finished — see
     * this class's javadoc for why a bare off-EDT {@code doClick()} is not safe here. Also drains
     * the EDT queue afterward with a no-op {@code invokeAndWait}: {@code AbstractStickyFireButton}
     * schedules its self-disable via its own {@code invokeLater} from inside a change listener, so
     * finishing our {@code doClick()} does not by itself guarantee that follow-up work has run yet
     * — submitting another task right after and waiting for it to complete does, since the EDT
     * processes its queue in order.
     */
    private static void clickOnEdt(AbstractButton button) {
        try {
            SwingUtilities.invokeAndWait(button::doClick);
            SwingUtilities.invokeAndWait(() -> {});
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while clicking " + button, e);
        } catch (InvocationTargetException e) {
            throw new AssertionError("Exception while clicking " + button, e.getCause());
        }
    }
}
