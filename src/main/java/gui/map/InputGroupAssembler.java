package gui.map;

import config.groups.GroupKey;
import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import config.loading.DeckMapControlsConfig;
import config.loading.DeckMapGroupsConfig;
import config.loading.DeckMapTopologyConfig;
import config.specification.GroupLayerSpec;
import domain.registry.TopologyModel;
import gui.map.input.AbstractToggleGroup;
import gui.map.input.DoorSealingButtonGroup;
import gui.map.input.EvacuationButtonGroup;
import gui.map.input.ExplosionButtonGroup;
import gui.map.input.ExtinguisherButtonGroup;
import gui.map.input.FireButtonGroup;
import gui.map.input.FireSensorButtonGroup;
import gui.map.input.FlammableButtonGroup;
import gui.map.input.MachineryDamageButtonGroup;
import gui.map.input.VentilationButtonGroup;
import gui.map.state.FireIncidentState;
import util.VisibleForTesting;

import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builds the map's <em>input</em> button groups (operator controls that drive {@code collect*Changes} /
 * {@code InputAction}s) from assembly + deck config, wires them into {@link FireIncidentState} and
 * {@link MapLayerVisibilityManager}, and exposes them for runtime collection and tests.
 * <p>
 * Hydrant label/button groups are assembled separately by {@link HydrantGroupAssembler}.
 * {@link DeckMapController} merges both assemblers' {@link #staticallyVisibleGroups()} and calls
 * {@link StaticallyVisible#addToMap} in assembly-config layer order.
 */
class InputGroupAssembler {
    // Null unless the assembly config declares the corresponding GroupKey.
    private final EvacuationButtonGroup evacuationGroup;
    private final VentilationButtonGroup ventilationGroup;
    private final DoorSealingButtonGroup doorSealingGroup;
    private final ExplosionButtonGroup explosionGroup;
    private final FlammableButtonGroup flammableGroup;
    private final MachineryDamageButtonGroup machineryDamageGroup;
    private final ExtinguisherButtonGroup extinguisherGroup;
    private final FireButtonGroup fireButtonGroup;
    private final FireSensorButtonGroup fireSensorGroup;

    private final Map<GroupKey, StaticallyVisible> staticallyVisibleGroups;

    // Private bag after assemble(); each group parameter is a distinct type — S107 not actionable.
    @SuppressWarnings("java:S107")
    private InputGroupAssembler(EvacuationButtonGroup evacuationGroup, VentilationButtonGroup ventilationGroup,
        DoorSealingButtonGroup doorSealingGroup, ExplosionButtonGroup explosionGroup,
        FlammableButtonGroup flammableGroup, MachineryDamageButtonGroup machineryDamageGroup,
        ExtinguisherButtonGroup extinguisherGroup, FireButtonGroup fireButtonGroup,
        FireSensorButtonGroup fireSensorGroup, Map<GroupKey, StaticallyVisible> staticallyVisibleGroups)
    {
        this.evacuationGroup = evacuationGroup;
        this.ventilationGroup = ventilationGroup;
        this.doorSealingGroup = doorSealingGroup;
        this.explosionGroup = explosionGroup;
        this.flammableGroup = flammableGroup;
        this.machineryDamageGroup = machineryDamageGroup;
        this.extinguisherGroup = extinguisherGroup;
        this.fireButtonGroup = fireButtonGroup;
        this.fireSensorGroup = fireSensorGroup;
        this.staticallyVisibleGroups = staticallyVisibleGroups;
    }

    static InputGroupAssembler assemble(DeckMapAssemblyConfig assemblyConfig, DeckMapConfig deckMapConfig,
        TopologyModel topology, ActionListener actionListener, FireIncidentState fireIncidentState,
        MapLayerVisibilityManager phaseVisibility)
    {
        DeckMapTopologyConfig topologyConfig = deckMapConfig.getTopologyConfig();
        DeckMapControlsConfig controlConfig = deckMapConfig.getControlConfig();
        DeckMapGroupsConfig groupsConfig = deckMapConfig.getGroupsConfig();

        // Constructed only for groups this deck's assembly config declares (see GroupLayerSpec /
        // config/assembly.yaml's "group-layers"). The shipped config declares every input group; tests
        // can pass a trimmed assembly config for a single-group "thematic" controller.
        Set<GroupKey> activeGroups = EnumSet.noneOf(GroupKey.class);
        for (GroupLayerSpec layer : assemblyConfig.groupLayerSpecs())
            activeGroups.add(layer.key());

        Map<GroupKey, StaticallyVisible> staticallyVisible = new EnumMap<>(GroupKey.class);

        FireSensorButtonGroup fireSensorGroup = buildActiveGroup(activeGroups, GroupKey.FIRE_SENSOR_GROUP,
            () -> new FireSensorButtonGroup(groupsConfig.fireSensorGroupConfig(), topology),
            null, actionListener, phaseVisibility, staticallyVisible);

        FireButtonGroup fireButtonGroup = buildActiveGroup(activeGroups, GroupKey.FIRE_BUTTON_GROUP,
            () -> new FireButtonGroup(controlConfig.fireButtons(), topology),
            null, actionListener, phaseVisibility, staticallyVisible);

        EvacuationButtonGroup evacuationGroup = buildActiveGroup(activeGroups, GroupKey.EVACUATION_GROUP,
            () -> new EvacuationButtonGroup(controlConfig.evacuationButtons(), topology),
            fireIncidentState::addInputControlListener, actionListener, phaseVisibility, staticallyVisible);

        VentilationButtonGroup ventilationGroup = buildActiveGroup(activeGroups, GroupKey.VENTILATION_GROUP,
            () -> new VentilationButtonGroup(groupsConfig.ventilationGroupConfig(), topology),
            fireIncidentState::addInputControlListener, actionListener, phaseVisibility, staticallyVisible);

        FlammableButtonGroup flammableGroup = buildActiveGroup(activeGroups, GroupKey.FLAMMABLE_GROUP,
            () -> new FlammableButtonGroup(groupsConfig.flammableGroupConfig(), topology),
            fireIncidentState::addInputControlListener, actionListener, phaseVisibility, staticallyVisible);

        MachineryDamageButtonGroup machineryDamageGroup = buildActiveGroup(activeGroups, GroupKey.MACHINERY_DAMAGE_GROUP,
            () -> new MachineryDamageButtonGroup(groupsConfig.machineryDamageGroupConfig(), topology),
            fireIncidentState::addInputControlListener, actionListener, phaseVisibility, staticallyVisible);

        DoorSealingButtonGroup doorSealingGroup = buildActiveGroup(activeGroups, GroupKey.DOOR_SEALING_GROUP,
            () -> new DoorSealingButtonGroup(controlConfig.doorButtons(), groupsConfig.doorButtonGroup(),
                topologyConfig.getFireRatedByDoorCode(), topology),
            fireIncidentState::addInputControlListener, actionListener, phaseVisibility, staticallyVisible);

        ExplosionButtonGroup explosionGroup = buildActiveGroup(activeGroups, GroupKey.EXPLOSION_GROUP,
            () -> new ExplosionButtonGroup(groupsConfig.explosionGroupConfig(), topology,
                fireIncidentState::setPreventedExplosionLocations),
            fireIncidentState::addExplosionControlListener, actionListener, phaseVisibility, staticallyVisible);

        // No topology.yaml coordinates exist yet for extinguishers — always built with zero elements,
        // so this group never creates a button (see ExtinguisherButtonGroup).
        ExtinguisherButtonGroup extinguisherGroup = buildActiveGroup(activeGroups, GroupKey.EXTINGUISHERS_GROUP,
            () -> new ExtinguisherButtonGroup(List.of(), topology),
            null, actionListener, phaseVisibility, staticallyVisible);

        return new InputGroupAssembler(evacuationGroup, ventilationGroup, doorSealingGroup, explosionGroup,
            flammableGroup, machineryDamageGroup, extinguisherGroup, fireButtonGroup, fireSensorGroup,
            Collections.unmodifiableMap(staticallyVisible));
    }

    /**
     * Builds and registers a group only if {@code activeGroups} declares {@code key}, otherwise returns
     * null. {@code wireListener} is the group's {@code fireIncidentState.addXxxListener} call, or null
     * for groups that don't listen (fire/fire-sensor dispatch directly; extinguishers have no placement
     * data yet).
     */
    private static <G extends AbstractToggleGroup<?, ?>> G buildActiveGroup(Set<GroupKey> activeGroups, GroupKey key,
        Supplier<G> factory, Consumer<G> wireListener, ActionListener actionListener,
        MapLayerVisibilityManager phaseVisibility, Map<GroupKey, StaticallyVisible> staticallyVisible)
    {
        if (!activeGroups.contains(key))
            return null;

        G group = factory.get();
        if (wireListener != null)
            wireListener.accept(group);
        group.addActionListener(actionListener);
        staticallyVisible.put(key, group);
        phaseVisibility.register(key, group);
        return group;
    }

    /**
     * Groups that participate in assembly-config {@code group-layers} {@code addToMap} ordering. Merged
     * by {@link DeckMapController} with the hydrant assembler's map.
     */
    Map<GroupKey, StaticallyVisible> staticallyVisibleGroups() {
        return staticallyVisibleGroups;
    }

    //================================================================
    // GROUP ACCESSORS — production API (collect*Changes)
    //================================================================

    EvacuationButtonGroup getEvacuationGroup() {
        return evacuationGroup;
    }

    VentilationButtonGroup getVentilationGroup() {
        return ventilationGroup;
    }

    DoorSealingButtonGroup getDoorSealingGroup() {
        return doorSealingGroup;
    }

    ExplosionButtonGroup getExplosionGroup() {
        return explosionGroup;
    }

    FlammableButtonGroup getFlammableGroup() {
        return flammableGroup;
    }

    MachineryDamageButtonGroup getMachineryDamageGroup() {
        return machineryDamageGroup;
    }

    ExtinguisherButtonGroup getExtinguisherGroup() {
        return extinguisherGroup;
    }

    //================================================================
    // GROUP ACCESSORS — test-only
    //================================================================

    @VisibleForTesting
    FireButtonGroup getFireButtonGroup() {
        return fireButtonGroup;
    }

    @VisibleForTesting
    FireSensorButtonGroup getFireSensorGroup() {
        return fireSensorGroup;
    }
}
