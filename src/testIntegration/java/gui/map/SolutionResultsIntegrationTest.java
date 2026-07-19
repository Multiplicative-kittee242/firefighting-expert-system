package gui.map;

import clips.ClipsReportService;
import config.groups.GroupKey;
import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import config.specification.GroupLayerSpec;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import fixtures.FakeClipsReadOnlyService;
import gui.Localization;
import gui.actions.ActionDispatcher;
import gui.actions.InputActionListener;
import gui.map.input.AbstractControlGroup;
import gui.solution.SolutionPhaseTree;
import gui.solution.SolutionResultsController;
import gui.solution.SolutionTreeSection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import util.ResourceUtil;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Direction 3.4: the real {@code SolutionPhaseTree} → {@code PhaseChangeListener} →
 * {@code SolutionResultsController} notify chain, driven by a real button click through the real
 * {@code ActionDispatcher}/{@code InputAction} wiring — not verified through a mocked {@code
 * SolutionPhaseTree}/{@code SolutionResultsController} (as {@code ClickThroughInputActionTest}
 * does, to lock down the refresh/reset call contract) but through the REAL objects, asserting the
 * REAL {@link SolutionResultsController#getActionsTable()} ends up with the right rows. That is a
 * materially different failure mode to catch: a bug where {@code refreshCurrentPhase} is called
 * correctly (so the mock-based assertion would pass) but the listener chain itself, or {@code
 * SolutionResultsController}'s own per-phase query logic, is wired wrong would slip through the
 * mock-only test and get caught here.
 * <p>
 * Lives in package {@code gui.map} (not {@code gui.solution}) so it can call
 * {@link DeckMapController}'s package-private {@code @VisibleForTesting} {@code inputGroups()}
 * accessors when collecting the real button instances to click.
 * <p>
 * Parameterized over every {@code collect*}-driven group whose data shape is a plain
 * {@code Set<Location>} keyed one-to-one to a single {@code ClipsReadOnlyService.collect*(String)}
 * call: Evacuation, Ventilation, Flammable, MachineryDamage, Explosion. {@code DOOR_SEALING_GROUP}
 * is deliberately not folded into that same parameterized case — it is {@link Link}-keyed, its
 * phase table is built from two separate collect calls (close / keep-open), and {@code
 * addSealingDoorKeepOpenRows} always drops the query's last element (a CLIPS list-boundary quirk
 * the other four groups have no equivalent of) — see {@link
 * #reportingADoorChange_RefreshesTheRealResultsTable()} for its own dedicated test instead.
 * <p>
 * {@code ClipsReadOnlyService} is a real, hand-written {@link FakeClipsReadOnlyService}, not a
 * {@code @Mock} — its fields are plain, visible Java state, settable via a fluent builder, so
 * "CLIPS's answer changes after the click" is expressed by literally re-calling the same setter
 * with a smaller set at the point in the test where that change should take effect, not by
 * registering two canned answers up front and trusting an interception framework to hand them out
 * in the right order (Mockito's {@code .thenReturn(before).thenReturn(after)}). {@code
 * ClipsReportService} stays a genuine {@code @Mock}: no test here reads its return value or needs
 * it to reflect changing state, only that calling it doesn't throw and that the real {@code
 * refreshCurrentPhase()} call after it still happens — exactly the case a mock (Mockito's own
 * strength for interaction-only collaborators) fits better than a fake would.
 * <p>
 * <b>Fixture split:</b> {@link #deckMapConfig}/{@link #topology}/{@link #realAssembly}/
 * {@link #mapImage} are read-only, group-independent, and identical for every test in this class —
 * built once in a {@code static} {@code @BeforeAll}, not per test. {@link #clipsReadOnlyService}/
 * {@link #solutionTree}/{@link #resultsController} carry state a test drives or reads (queried
 * locations, selected phase, populated table) and must not leak between tests, so they are rebuilt
 * fresh in {@code @BeforeEach} — after {@code @Mock} fields are already reset for this invocation
 * (Mockito's own {@code beforeEach} callback runs before any user-defined one), so {@link
 * #resultsController} always wraps this invocation's own fresh {@link #clipsReadOnlyService}. The
 * {@code DeckMapController} itself cannot move to {@code @BeforeEach} either way — which group is
 * thematically active differs per test case — so {@link #buildThematicController} builds it per
 * test from the shared {@link #solutionTree}/{@link #resultsController} fields. Its {@code
 * InputActionListener} is deliberately not a shared field at all (unlike the three above): each
 * test constructs its own right where it's used and hands it straight to {@link
 * #buildThematicController} — a short-lived wiring detail worth keeping visible at the call site,
 * not a real stateful collaborator worth hiding behind {@code @BeforeEach}.
 */
@ExtendWith(MockitoExtension.class)
class SolutionResultsIntegrationTest {

    @Mock
    private ClipsReportService clipsReportService;

    private static DeckMapConfig deckMapConfig;
    private static TopologyModel topology;
    private static DeckMapAssemblyConfig realAssembly;
    private static ImageIcon mapImage;

    @BeforeAll
    static void loadSharedFixtures() {
        deckMapConfig = DeckMapConfig.createDefault();
        topology = deckMapConfig.getTopologyConfig().buildTopologyModel();
        realAssembly = DeckMapAssemblyConfig.createDefault(deckMapConfig.getGroupsConfig(), topology.allHydrantOutlets());
        mapImage = new ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()));
    }

    private FakeClipsReadOnlyService clipsReadOnlyService;
    private SolutionPhaseTree solutionTree;
    private SolutionResultsController resultsController;

    @BeforeEach
    void wireFreshControllerChain() {
        clipsReadOnlyService = FakeClipsReadOnlyService.fakeClips();
        solutionTree = new SolutionPhaseTree();
        resultsController = new SolutionResultsController(clipsReadOnlyService, topology);
        solutionTree.addPhaseChangeListener(resultsController);
    }

    /**
     * Builds a thematic (single-group) {@link DeckMapController}, wired to this test's shared
     * {@link #solutionTree}/{@link #resultsController} via a real {@link ActionDispatcher}. The
     * one piece of setup that cannot move to {@code @BeforeEach} — which group is active differs
     * per test case — so every test calls this first with its own key and its own freshly
     * constructed {@code listener}.
     */
    private DeckMapController buildThematicController(GroupKey groupKey, InputActionListener listener) {
        DeckMapAssemblyConfig thematicAssembly = new DeckMapAssemblyConfig(
            List.of(new GroupLayerSpec(groupKey, true)), realAssembly.hydrantButtonGroupSpecList());
        DeckMapController deckMapController = new DeckMapController(thematicAssembly, deckMapConfig, topology, mapImage, listener);
        listener.setDispatcher(new ActionDispatcher(clipsReportService, deckMapController, solutionTree, resultsController));
        return deckMapController;
    }

    private static Stream<GroupCase> reportingAChange_RefreshesTheRealResultsTable() {
        return Stream.of(
            new GroupCase(GroupKey.EVACUATION_GROUP, SolutionTreeSection.EVACUATION,
                controller -> collectButtons(controller.inputGroups().getEvacuationGroup()),
                FakeClipsReadOnlyService::withEvacuation),
            new GroupCase(GroupKey.VENTILATION_GROUP, SolutionTreeSection.SEALING,
                controller -> collectButtons(controller.inputGroups().getVentilationGroup()),
                FakeClipsReadOnlyService::withVentilationOff),
            new GroupCase(GroupKey.FLAMMABLE_GROUP, SolutionTreeSection.PREVENTION,
                controller -> collectButtons(controller.inputGroups().getFlammableGroup()),
                FakeClipsReadOnlyService::withIsolationPhase),
            new GroupCase(GroupKey.MACHINERY_DAMAGE_GROUP, SolutionTreeSection.PREVENTION,
                controller -> collectButtons(controller.inputGroups().getMachineryDamageGroup()),
                FakeClipsReadOnlyService::withMachineryDamage),
            new GroupCase(GroupKey.EXPLOSION_GROUP, SolutionTreeSection.PREVENTION,
                controller -> collectButtons(controller.inputGroups().getExplosionGroup()),
                FakeClipsReadOnlyService::withExplosionPhase)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void reportingAChange_RefreshesTheRealResultsTable(GroupCase testCase) {
        InputActionListener listener = new InputActionListener();
        DeckMapController deckMapController = buildThematicController(testCase.groupKey(), listener);

        Map<Location, AbstractButton> buttons = testCase.buttonsAccessor().apply(deckMapController);
        Location reported = buttons.keySet().iterator().next();
        Location stillPending = buttons.keySet().stream().filter(l -> !l.equals(reported)).findFirst().orElseThrow();

        testCase.applyLocations().accept(clipsReadOnlyService, Set.of(reported, stillPending));

        // Selecting the group's tab (as the operator would) fires the tree's own selection
        // listener -> notifyPhaseChanged -> the fake's current answer.
        selectSection(solutionTree, testCase.phase());

        JTable actionsTable = resultsController.getActionsTable();
        assertThat(actionsTable.getRowCount(), is(2));
        assertThat(roomColumnValues(actionsTable), containsInAnyOrder(
            reported.getCode().toUpperCase(), stillPending.getCode().toUpperCase()));

        // Mutate the fake to reflect CLIPS's own state once the click is handled, THEN click — a
        // real click on the reported location's real button, through the real InputAction/
        // ActionDispatcher chain (the report* call itself is a mocked no-op); only
        // refreshCurrentPhase's downstream re-query of the now-mutated fake is under test here.
        testCase.applyLocations().accept(clipsReadOnlyService, Set.of(stillPending));
        clickOnEdt(buttons.get(reported));

        assertThat(actionsTable.getRowCount(), is(1));
        assertThat(roomColumnValues(actionsTable), containsInAnyOrder(stillPending.getCode().toUpperCase()));
    }

    /**
     * Direction 3.4, door-sealing case: {@code DOOR_SEALING_GROUP} is excluded from the
     * parameterized {@link #reportingAChange_RefreshesTheRealResultsTable} family above (see the
     * class javadoc for why) but the
     * same real-click / real-table contract still applies, against {@code sealingDoorsToClose}
     * instead of a {@code Set<Location>} field. {@code ventilationOff}/{@code sealingDoorsKeepOpen}
     * are the SEALING phase's other two queries — left at the fake's default empty value so only
     * the door-close rows under test appear.
     * <p>
     * Like every test in this class, this drives the chain through exactly one representative
     * button pair ({@code reported}/{@code stillPending}), not every door in the group. Per-button
     * wiring correctness (does button N really carry the InputAction for door N, is any door
     * silently unwired) is {@code ClickThroughInputActionTest}'s job, proven there against a mocked
     * {@code SolutionPhaseTree}/{@code SolutionResultsController} with an exact per-group click
     * count read back from config. This class's own failure mode — the listener chain and
     * per-phase query logic — does not vary by which door fired the click, so one representative
     * pair fully exercises it; looping over every door here would re-prove per-button wiring a
     * second time without covering anything new, at the cost of a heavier fixture (a full {@code
     * DeckMapController} construction, or loop iteration, per door instead of once).
     */
    @Test
    void reportingADoorChange_RefreshesTheRealResultsTable() {
        InputActionListener listener = new InputActionListener();
        DeckMapController deckMapController = buildThematicController(GroupKey.DOOR_SEALING_GROUP, listener);

        Map<Link, AbstractButton> buttons = collectButtons(deckMapController.inputGroups().getDoorSealingGroup());
        Link reported = buttons.keySet().iterator().next();
        Link stillPending = buttons.keySet().stream().filter(l -> !l.equals(reported)).findFirst().orElseThrow();

        clipsReadOnlyService.withSealingDoorsToClose(List.of(reported, stillPending));

        selectSection(solutionTree, SolutionTreeSection.SEALING);

        JTable actionsTable = resultsController.getActionsTable();
        assertThat(actionsTable.getRowCount(), is(2));
        assertThat(roomColumnValues(actionsTable), containsInAnyOrder(doorLabel(reported), doorLabel(stillPending)));

        clipsReadOnlyService.withSealingDoorsToClose(List.of(stillPending));
        clickOnEdt(buttons.get(reported));

        assertThat(actionsTable.getRowCount(), is(1));
        assertThat(roomColumnValues(actionsTable), containsInAnyOrder(doorLabel(stillPending)));
    }

    /**
     * LOCALIZATION is not a {@code collect*Changes}-driven group's own tab — no button reports
     * directly into it (the dynamic hydrant button groups have no wired {@code ActionListener} at
     * all, see {@code gui.map.view.FireHoseButtonGroup}'s javadoc) — but its {@code onPhaseChanged}
     * branch is the most involved of the whole switch: a non-empty guard over three unrelated
     * collections (isolation / machinery-stop / explosion), then a loop over every fire-line
     * location building hydrant rows via {@code getHydrantOutletsForLocation}/{@code
     * processFireLineHydrants}. Each of the three collections is fed by a DIFFERENT group's own
     * click ({@code FlammableActionInput}/{@code MachineryDamageActionInput}/{@code
     * ExplosionPreventionActionInput}), and every one of them calls {@code
     * solutionTree.refreshCurrentPhase()} unconditionally — which re-fires whichever phase is
     * currently selected, LOCALIZATION here, exactly like an operator who clicks one of these
     * buttons while viewing the localization tab. Parameterized over all three drivers so the test
     * proves the shared LOCALIZATION block is reachable and correctly re-queried through each of
     * its three independent entry points, not just one arbitrarily chosen one.
     * <p>
     * The fire-line/hydrant-outlet data is static background for every case here (the click never
     * changes it) — a single fire-line location with one allocated hydrant outlet, contributing one
     * constant row alongside the two rows the click-driven collection contributes.
     */
    private static Stream<LocalizationCase> reportingALocalizationChange_RefreshesTheRealLocalizationTable() {
        return Stream.of(
            new LocalizationCase(GroupKey.FLAMMABLE_GROUP,
                controller -> collectButtons(controller.inputGroups().getFlammableGroup()),
                FakeClipsReadOnlyService::withIsolationPhase),
            new LocalizationCase(GroupKey.MACHINERY_DAMAGE_GROUP,
                controller -> collectButtons(controller.inputGroups().getMachineryDamageGroup()),
                FakeClipsReadOnlyService::withMachineryDamage),
            new LocalizationCase(GroupKey.EXPLOSION_GROUP,
                controller -> collectButtons(controller.inputGroups().getExplosionGroup()),
                FakeClipsReadOnlyService::withExplosionPhase)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void reportingALocalizationChange_RefreshesTheRealLocalizationTable(LocalizationCase testCase) {
        InputActionListener listener = new InputActionListener();
        DeckMapController deckMapController = buildThematicController(testCase.groupKey(), listener);

        Map<Location, AbstractButton> buttons = testCase.buttonsAccessor().apply(deckMapController);
        Location reported = buttons.keySet().iterator().next();
        Location stillPending = buttons.keySet().stream().filter(l -> !l.equals(reported)).findFirst().orElseThrow();

        // Fire-line hydrant allocation is static background data here, not the thing the click
        // changes — a single fire-line location with one allocated hydrant outlet.
        Location fireLineLocation = topology.location("q");
        HydrantOutlets hydrantOutlet = topology.hydrantOutlets("hydr_q");
        clipsReadOnlyService.withFireLineLocations(Set.of(fireLineLocation));
        clipsReadOnlyService.withHydrantOutletsByLocation(Map.of(fireLineLocation, List.of(hydrantOutlet)));

        testCase.applyLocations().accept(clipsReadOnlyService, Set.of(reported, stillPending));

        selectSection(solutionTree, SolutionTreeSection.LOCALIZATION);

        JTable actionsTable = resultsController.getActionsTable();
        assertThat(actionsTable.getRowCount(), is(3));
        assertThat(roomColumnValues(actionsTable), containsInAnyOrder(
            fireLineLocation.getCode().toUpperCase(), reported.getCode().toUpperCase(), stillPending.getCode().toUpperCase()));

        testCase.applyLocations().accept(clipsReadOnlyService, Set.of(stillPending));
        clickOnEdt(buttons.get(reported));

        assertThat(actionsTable.getRowCount(), is(2));
        assertThat(roomColumnValues(actionsTable), containsInAnyOrder(
            fireLineLocation.getCode().toUpperCase(), stillPending.getCode().toUpperCase()));
    }

    /** Mirrors {@code SolutionResultsController}'s private {@code DoorLabel#toDisplayString}, not reachable from here. */
    private static String doorLabel(Link door) {
        return door.getFrom().getCode().toUpperCase() + ", " + door.getTo().getCode().toUpperCase();
    }

    private static <D, T extends AbstractButton> Map<D, AbstractButton> collectButtons(AbstractControlGroup<T, D> group) {
        Map<D, AbstractButton> buttons = new LinkedHashMap<>();
        group.forEachControl(buttons::put);
        return buttons;
    }

    private static List<String> roomColumnValues(JTable actionsTable) {
        List<String> rooms = new ArrayList<>();
        for (int row = 0; row < actionsTable.getRowCount(); row++)
            rooms.add((String) actionsTable.getValueAt(row, 2));
        return rooms;
    }

    private static void selectSection(SolutionPhaseTree solutionTree, SolutionTreeSection section) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) solutionTree.getPhasesTree().getModel().getRoot();
        Enumeration<TreeNode> nodes = root.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof SolutionPhaseTree.ResultsTreeNode data && data.section() == section) {
                solutionTree.getPhasesTree().setSelectionPath(new TreePath(node.getPath()));
                return;
            }
        }
        throw new IllegalStateException("Section not found in tree: " + section);
    }

    /** Same EDT-safety rationale as {@code ClickThroughInputActionTest#clickOnEdt}. */
    private static void clickOnEdt(AbstractButton button) {
        try {
            SwingUtilities.invokeAndWait(button::doClick);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while clicking " + button, e);
        } catch (InvocationTargetException e) {
            throw new AssertionError("Exception while clicking " + button, e.getCause());
        }
    }

    private record GroupCase(GroupKey groupKey, SolutionTreeSection phase,
        Function<DeckMapController, Map<Location, AbstractButton>> buttonsAccessor,
        BiConsumer<FakeClipsReadOnlyService, Set<Location>> applyLocations)
    {
        @Override
        public String toString() {
            return groupKey.toString();
        }
    }

    private record LocalizationCase(GroupKey groupKey,
        Function<DeckMapController, Map<Location, AbstractButton>> buttonsAccessor,
        BiConsumer<FakeClipsReadOnlyService, Set<Location>> applyLocations)
    {
        @Override
        public String toString() {
            return groupKey.toString();
        }
    }
}
