package gui.map;

import config.groups.GroupKey;
import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import config.loading.DeckMapGroupsConfig;
import config.specification.HydrantButtonGroupSpec;
import domain.registry.TopologyModel;
import gui.map.state.FireIncidentState;
import gui.map.view.AbstractHydrantLabelGroup;
import gui.map.view.FireHoseButtonGroup;
import gui.map.view.FirefightingStepGroup;
import gui.map.view.FrontlineBalanceGroup;
import gui.map.view.HydrExtBButtonGroup;
import gui.map.view.HydrExtBFromButtonGroup;
import gui.map.view.HydrExtButtonGroup;
import gui.map.view.HydrantButtonGroup;
import gui.map.view.HydrantOutletsGroup;
import util.VisibleForTesting;

import javax.swing.JLabel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Builds the map's <em>hydrant</em> view groups (static labels and dynamic hose/ext buttons) from
 * assembly + groups config, wires them into {@link FireIncidentState} and
 * {@link MapLayerVisibilityManager}, and exposes them for tests.
 * <p>
 * Input controls are assembled by {@link InputGroupAssembler}. Static hydrant labels contribute to
 * {@link #staticallyVisibleGroups()} for the controller's ordered {@code addToMap} pass; dynamic
 * hydrant button groups only register with the phase-visibility manager (they attach to the map label
 * themselves when hydrant view data arrives).
 */
class HydrantGroupAssembler {
    private final FrontlineBalanceGroup frontlineBalanceGroup;
    private final HydrantOutletsGroup hydrantOutletsGroup;
    private final FirefightingStepGroup firefightingStepGroup;
    private final FireHoseButtonGroup fireHoseButtonGroup;
    private final HydrExtButtonGroup hydrExtButtonGroup;
    private final HydrExtBButtonGroup hydrExtBButtonGroup;
    private final HydrExtBFromButtonGroup hydrExtBFromButtonGroup;

    private final Map<GroupKey, StaticallyVisible> staticallyVisibleGroups;

    // Private bag after assemble(); each group parameter is a distinct type — S107 not actionable.
    @SuppressWarnings("java:S107")
    private HydrantGroupAssembler(FrontlineBalanceGroup frontlineBalanceGroup, HydrantOutletsGroup hydrantOutletsGroup,
        FirefightingStepGroup firefightingStepGroup, FireHoseButtonGroup fireHoseButtonGroup,
        HydrExtButtonGroup hydrExtButtonGroup, HydrExtBButtonGroup hydrExtBButtonGroup,
        HydrExtBFromButtonGroup hydrExtBFromButtonGroup, Map<GroupKey, StaticallyVisible> staticallyVisibleGroups)
    {
        this.frontlineBalanceGroup = frontlineBalanceGroup;
        this.hydrantOutletsGroup = hydrantOutletsGroup;
        this.firefightingStepGroup = firefightingStepGroup;
        this.fireHoseButtonGroup = fireHoseButtonGroup;
        this.hydrExtButtonGroup = hydrExtButtonGroup;
        this.hydrExtBButtonGroup = hydrExtBButtonGroup;
        this.hydrExtBFromButtonGroup = hydrExtBFromButtonGroup;
        this.staticallyVisibleGroups = staticallyVisibleGroups;
    }

    static HydrantGroupAssembler assemble(DeckMapAssemblyConfig assemblyConfig, DeckMapConfig deckMapConfig,
        TopologyModel topology, JLabel mapLabel, FireIncidentState fireIncidentState,
        MapLayerVisibilityManager phaseVisibility)
    {
        DeckMapGroupsConfig groupsConfig = deckMapConfig.getGroupsConfig();
        Map<GroupKey, StaticallyVisible> staticallyVisible = new EnumMap<>(GroupKey.class);

        // Static label groups — always built (not gated by group-layers; out of scope for thematic
        // input-only tests). They listen for hydrant view updates and participate in addToMap.
        FrontlineBalanceGroup frontlineBalanceGroup =
            new FrontlineBalanceGroup(groupsConfig.frontlineBalanceGroupConfig(), topology);
        registerHydrantStaticGroup(GroupKey.FRONTLINE_BALANCE_GROUP, frontlineBalanceGroup, fireIncidentState,
            phaseVisibility, staticallyVisible);

        HydrantOutletsGroup hydrantOutletsGroup =
            new HydrantOutletsGroup(groupsConfig.hydrOutletLabelGroupConfig(), topology);
        registerHydrantStaticGroup(GroupKey.HYDRANT_OUTLETS_GROUP, hydrantOutletsGroup, fireIncidentState,
            phaseVisibility, staticallyVisible);

        FirefightingStepGroup firefightingStepGroup =
            new FirefightingStepGroup(groupsConfig.firefightingStepGroupConfig(), topology);
        registerHydrantStaticGroup(GroupKey.FIREFIGHTING_STEPS_GROUP, firefightingStepGroup, fireIncidentState,
            phaseVisibility, staticallyVisible);

        // Dynamic button groups — keyed off hydrant-button-groups specs, not group-layers.
        Map<GroupKey, HydrantButtonGroupSpec> hydrantSpecifications = assemblyConfig.hydrantButtonGroupSpecs();

        FireHoseButtonGroup fireHoseButtonGroup =
            new FireHoseButtonGroup(hydrantSpecifications.get(GroupKey.FIRE_HOSE), topology, mapLabel);
        registerHydrantDynamicGroup(GroupKey.FIRE_HOSE, fireHoseButtonGroup, fireIncidentState, phaseVisibility);

        HydrExtButtonGroup hydrExtButtonGroup =
            new HydrExtButtonGroup(hydrantSpecifications.get(GroupKey.HYDR_EXT), topology, mapLabel);
        registerHydrantDynamicGroup(GroupKey.HYDR_EXT, hydrExtButtonGroup, fireIncidentState, phaseVisibility);

        HydrExtBButtonGroup hydrExtBButtonGroup =
            new HydrExtBButtonGroup(hydrantSpecifications.get(GroupKey.HYDR_EXT_B), topology, mapLabel);
        registerHydrantDynamicGroup(GroupKey.HYDR_EXT_B, hydrExtBButtonGroup, fireIncidentState, phaseVisibility);

        HydrExtBFromButtonGroup hydrExtBFromButtonGroup =
            new HydrExtBFromButtonGroup(hydrantSpecifications.get(GroupKey.HYDR_EXT_B_FROM), topology, mapLabel);
        registerHydrantDynamicGroup(GroupKey.HYDR_EXT_B_FROM, hydrExtBFromButtonGroup, fireIncidentState, phaseVisibility);

        return new HydrantGroupAssembler(frontlineBalanceGroup, hydrantOutletsGroup, firefightingStepGroup,
            fireHoseButtonGroup, hydrExtButtonGroup, hydrExtBButtonGroup, hydrExtBFromButtonGroup,
            Collections.unmodifiableMap(staticallyVisible));
    }

    private static void registerHydrantStaticGroup(GroupKey key, AbstractHydrantLabelGroup<?, ?> group,
        FireIncidentState fireIncidentState, MapLayerVisibilityManager phaseVisibility,
        Map<GroupKey, StaticallyVisible> staticallyVisible)
    {
        fireIncidentState.addHydrantViewListener(group);
        staticallyVisible.put(key, group);
        phaseVisibility.register(key, group);
    }

    private static void registerHydrantDynamicGroup(GroupKey key, HydrantButtonGroup group,
        FireIncidentState fireIncidentState, MapLayerVisibilityManager phaseVisibility)
    {
        fireIncidentState.addHydrantViewListener(group);
        phaseVisibility.register(key, group);
    }

    /**
     * Static hydrant label groups for assembly-config {@code group-layers} {@code addToMap}. Merged by
     * {@link DeckMapController} with the input assembler's map.
     */
    Map<GroupKey, StaticallyVisible> staticallyVisibleGroups() {
        return staticallyVisibleGroups;
    }

    //================================================================
    // GROUP ACCESSORS — test-only (production never reads these back)
    //================================================================

    @VisibleForTesting
    FrontlineBalanceGroup getFrontlineBalanceGroup() {
        return frontlineBalanceGroup;
    }

    @VisibleForTesting
    HydrantOutletsGroup getHydrantOutletsGroup() {
        return hydrantOutletsGroup;
    }

    @VisibleForTesting
    FirefightingStepGroup getFirefightingStepGroup() {
        return firefightingStepGroup;
    }

    @VisibleForTesting
    FireHoseButtonGroup getFireHoseButtonGroup() {
        return fireHoseButtonGroup;
    }

    @VisibleForTesting
    HydrExtButtonGroup getHydrExtButtonGroup() {
        return hydrExtButtonGroup;
    }

    @VisibleForTesting
    HydrExtBButtonGroup getHydrExtBButtonGroup() {
        return hydrExtBButtonGroup;
    }

    @VisibleForTesting
    HydrExtBFromButtonGroup getHydrExtBFromButtonGroup() {
        return hydrExtBFromButtonGroup;
    }
}
