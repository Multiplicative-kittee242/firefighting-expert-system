package gui.map;

import clips.FireIncidentSnapshot;
import config.groups.GroupKey;
import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import domain.FirefightingStep;
import domain.FrontlineHydrantsBalance;
import domain.HydrantOutlets;
import domain.HydrantState;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import gui.Localization;
import gui.map.input.AbstractControlGroup;
import gui.map.view.HydrantButtonGroup;
import gui.solution.SolutionTreeSection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import util.ResourceUtil;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code DeckMapController#onPhaseChanged} → {@code MapLayerVisibilityManager#apply} for
 * every {@link GroupKey} the rule table names — the six {@code collect*}-driven input groups plus
 * the three static hydrant/label groups and the four dynamic hydrant button groups — across every
 * {@link SolutionTreeSection}. The rule table itself had never been exercised by any test before
 * this class. Distinct from {@link RepresentFireInputControlsTest} (which proves {@code
 * representFire} reveals the *right* buttons within one group) and {@link
 * ClickThroughInputActionTest}/{@link SolutionResultsIntegrationTest} (button clicks and the results
 * table) — this is about which of several already-enabled groups the operator's currently-selected
 * phase tab shows or hides.
 * <p>
 * A real (not thematic/trimmed) {@link DeckMapController} is required: the rules are keyed by every
 * {@link GroupKey} at once. One button/label per group is enabled, once, via a single combined
 * {@code representFire} snapshot in {@code @BeforeAll} — enabling is orthogonal to the
 * phase-visibility question this class checks, so it only needs to happen once, not per phase.
 * <p>
 * {@code HYDR_EXT}/{@code HYDR_EXT_B}/{@code HYDR_EXT_B_FROM} never receive real data from CLIPS
 * (their backing snapshot fields are empty across every known scenario — see {@code
 * clips/README.md}'s "Border-routed hydrant assignment" section) — the snapshot values feeding them
 * here are synthetic (any valid {@link HydrantOutlets}/{@link Location}, since {@code
 * DeckMapAssemblyConfig#createDefault} gives all four hydrant button groups the same full outlet
 * set), included so the Java-side visibility wiring is verified independently of that CLIPS gap.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhaseVisibilityTest {

    private DeckMapController controller;
    private final Map<GroupKey, JComponent> enabledButtonByGroup = new EnumMap<>(GroupKey.class);

    @BeforeAll
    void beforeAll() {
        DeckMapConfig deckMapConfig = DeckMapConfig.createDefault();
        TopologyModel topology = deckMapConfig.getTopologyConfig().buildTopologyModel();
        DeckMapAssemblyConfig assemblyConfig = DeckMapAssemblyConfig.createDefault(
            deckMapConfig.getGroupsConfig(), topology.allHydrantOutlets());
        ImageIcon mapImage = new ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()));
        controller = new DeckMapController(assemblyConfig, deckMapConfig, topology, mapImage, event -> {});

        Location evacLocation = firstKey(collectButtons(controller.inputGroups().getEvacuationGroup()));
        Location ventLocation = firstKey(collectButtons(controller.inputGroups().getVentilationGroup()));
        Link door = firstKey(collectButtons(controller.inputGroups().getDoorSealingGroup()));
        Location flammableLocation = firstKey(collectButtons(controller.inputGroups().getFlammableGroup()));
        Location machineryLocation = firstKey(collectButtons(controller.inputGroups().getMachineryDamageGroup()));
        Location explosionLocation = firstKey(collectButtons(controller.inputGroups().getExplosionGroup()));

        // Real, golden-scenario-A-matching data for the fire-line/hydrant-outlet fields (see
        // FireScenarios in testClips for the same values cross-checked against the real engine).
        Location fireLineLocation = topology.location("q");
        Location fireLocation = topology.location("a");
        HydrantOutlets hydrantOutlet = topology.hydrantOutlets("hydr_q");

        FireIncidentSnapshot snapshot = mock(FireIncidentSnapshot.class);
        when(snapshot.evacuationLocations()).thenReturn(Set.of(evacLocation));
        when(snapshot.ventilationOffLocations()).thenReturn(Set.of(ventLocation));
        when(snapshot.sealingDoorsToClose()).thenReturn(List.of(door));
        when(snapshot.flammableLocations()).thenReturn(Set.of(flammableLocation));
        when(snapshot.machineryDamageLocations()).thenReturn(Set.of(machineryLocation));
        when(snapshot.explosionThreatLocations()).thenReturn(Set.of(explosionLocation));

        when(snapshot.fireLineLocations()).thenReturn(Set.of(fireLineLocation));
        when(snapshot.frontlineHydrantsBalance()).thenReturn(Map.of(fireLineLocation, new FrontlineHydrantsBalance(1, 0)));
        when(snapshot.fireLineHydrantOutletsByLocation()).thenReturn(Map.of(fireLineLocation, List.of(hydrantOutlet)));
        when(snapshot.hydrantOutletsState()).thenReturn(Map.of(hydrantOutlet, new HydrantState(hydrantOutlet, 2, 1)));
        when(snapshot.fireLocations()).thenReturn(Set.of(fireLocation));
        when(snapshot.firefightingPlanSteps()).thenReturn(Map.of(fireLocation, new FirefightingStep(fireLineLocation, 1)));
        // Synthetic (never populated by real CLIPS — see class javadoc): reuses fireLocation/hydrantOutlet.
        when(snapshot.extByLocation()).thenReturn(Map.of(fireLocation, List.of(hydrantOutlet)));
        when(snapshot.extBToByLocation()).thenReturn(Map.of(fireLocation, List.of(hydrantOutlet)));
        when(snapshot.graphFromLocations()).thenReturn(Set.of(fireLocation));
        when(snapshot.extBFromByLocation()).thenReturn(Map.of(fireLocation, List.of(hydrantOutlet)));

        controller.representFire(snapshot);

        enabledButtonByGroup.put(GroupKey.EVACUATION_GROUP, collectButtons(controller.inputGroups().getEvacuationGroup()).get(evacLocation));
        enabledButtonByGroup.put(GroupKey.VENTILATION_GROUP, collectButtons(controller.inputGroups().getVentilationGroup()).get(ventLocation));
        enabledButtonByGroup.put(GroupKey.DOOR_SEALING_GROUP, collectButtons(controller.inputGroups().getDoorSealingGroup()).get(door));
        enabledButtonByGroup.put(GroupKey.FLAMMABLE_GROUP, collectButtons(controller.inputGroups().getFlammableGroup()).get(flammableLocation));
        enabledButtonByGroup.put(GroupKey.MACHINERY_DAMAGE_GROUP, collectButtons(controller.inputGroups().getMachineryDamageGroup()).get(machineryLocation));
        enabledButtonByGroup.put(GroupKey.EXPLOSION_GROUP, collectButtons(controller.inputGroups().getExplosionGroup()).get(explosionLocation));

        enabledButtonByGroup.put(GroupKey.FRONTLINE_BALANCE_GROUP, collectButtons(controller.hydrantGroups().getFrontlineBalanceGroup()).get(fireLineLocation));
        enabledButtonByGroup.put(GroupKey.HYDRANT_OUTLETS_GROUP, collectButtons(controller.hydrantGroups().getHydrantOutletsGroup()).get(hydrantOutlet));
        enabledButtonByGroup.put(GroupKey.FIREFIGHTING_STEPS_GROUP, collectButtons(controller.hydrantGroups().getFirefightingStepGroup()).get(fireLocation));
        enabledButtonByGroup.put(GroupKey.FIRE_HOSE, onlyButton(controller.hydrantGroups().getFireHoseButtonGroup()));
        enabledButtonByGroup.put(GroupKey.HYDR_EXT, onlyButton(controller.hydrantGroups().getHydrExtButtonGroup()));
        enabledButtonByGroup.put(GroupKey.HYDR_EXT_B, onlyButton(controller.hydrantGroups().getHydrExtBButtonGroup()));
        enabledButtonByGroup.put(GroupKey.HYDR_EXT_B_FROM, onlyButton(controller.hydrantGroups().getHydrExtBFromButtonGroup()));

        for (Map.Entry<GroupKey, JComponent> entry : enabledButtonByGroup.entrySet()) {
            assertThat("setup: " + entry.getKey() + "'s button/label must be enabled before phase checks begin",
                entry.getValue().isEnabled(), is(true));
        }
    }

    private static Stream<Arguments> onPhaseChanged_ShowsOnlyTheGroupsThisPhaseNames() {
        return Stream.of(
            Arguments.of(SolutionTreeSection.ROOT, EnumSet.noneOf(GroupKey.class)),
            Arguments.of(SolutionTreeSection.PRIORITY_MEASURES,
                EnumSet.of(GroupKey.EVACUATION_GROUP, GroupKey.VENTILATION_GROUP, GroupKey.DOOR_SEALING_GROUP)),
            Arguments.of(SolutionTreeSection.EVACUATION, EnumSet.of(GroupKey.EVACUATION_GROUP)),
            Arguments.of(SolutionTreeSection.SEALING,
                EnumSet.of(GroupKey.VENTILATION_GROUP, GroupKey.DOOR_SEALING_GROUP)),
            Arguments.of(SolutionTreeSection.LOCALIZATION,
                EnumSet.of(GroupKey.FRONTLINE_BALANCE_GROUP, GroupKey.HYDRANT_OUTLETS_GROUP, GroupKey.FIRE_HOSE)),
            Arguments.of(SolutionTreeSection.PREVENTION,
                EnumSet.of(GroupKey.EXPLOSION_GROUP, GroupKey.FLAMMABLE_GROUP, GroupKey.MACHINERY_DAMAGE_GROUP)),
            Arguments.of(SolutionTreeSection.FIREFIGHTING,
                EnumSet.of(GroupKey.FIREFIGHTING_STEPS_GROUP, GroupKey.HYDR_EXT, GroupKey.HYDR_EXT_B, GroupKey.HYDR_EXT_B_FROM))
        );
    }

    @ParameterizedTest(name = "phase {0}")
    @MethodSource
    void onPhaseChanged_ShowsOnlyTheGroupsThisPhaseNames(SolutionTreeSection phase, Set<GroupKey> expectedVisible) {
        controller.onPhaseChanged(phase);

        for (Map.Entry<GroupKey, JComponent> entry : enabledButtonByGroup.entrySet()) {
            boolean shouldBeVisible = expectedVisible.contains(entry.getKey());
            assertThat("phase " + phase + ", group " + entry.getKey(),
                entry.getValue().isVisible(), is(shouldBeVisible));
        }
    }

    private static <D, T extends JComponent> Map<D, T> collectButtons(AbstractControlGroup<T, D> group) {
        Map<D, T> buttons = new LinkedHashMap<>();
        group.forEachControl(buttons::put);
        return buttons;
    }

    private static <D> D firstKey(Map<D, ?> map) {
        return map.keySet().iterator().next();
    }

    private static AbstractButton onlyButton(HydrantButtonGroup group) {
        List<? extends AbstractButton> buttons = group.getButtons();
        if (buttons.isEmpty())
            throw new IllegalStateException("expected exactly one button, got none");
        return buttons.get(0);
    }
}
