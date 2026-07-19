package gui.map;

import clips.FireIncidentSnapshot;
import clips.values.DoorState;
import clips.values.EvacuationStatus;
import clips.values.MachineryDamageAction;
import clips.values.VentilationAction;
import config.groups.GroupKey;
import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import config.loading.DeckMapControlsConfig;
import config.loading.DeckMapGeometryConfig;
import config.specification.GroupLayerSpec;
import domain.Extinguisher;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import domain.types.ExplosiveType;
import domain.types.PreventionType;
import gui.map.input.DoorSealingButtonGroup;
import gui.map.input.EvacuationButtonGroup;
import gui.map.input.ExplosionButtonGroup;
import gui.map.input.ExtinguisherButtonGroup;
import gui.map.input.FlammableButtonGroup;
import gui.map.input.MachineryDamageButtonGroup;
import gui.map.input.VentilationButtonGroup;
import gui.map.state.FireIncidentState;
import gui.map.state.HydrantViewData;
import gui.map.state.InputControlsData;
import gui.map.state.PaintingViewData;
import gui.map.view.painting.DeckMapGeometry;
import gui.map.view.painting.MapPainter;
import gui.map.values.ExtinguisherUsage;
import gui.solution.SolutionPhaseTree;
import gui.solution.SolutionTreeSection;
import util.VisibleForTesting;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class DeckMapController implements SolutionPhaseTree.PhaseChangeListener {
    private static final int FIRE_LINE_WIDTH = 12;

    private final FireIncidentState fireIncidentState;
    private final MapLayerVisibilityManager phaseVisibility;
    private final MapPainter mapPainter;
    private final InputGroupAssembler inputGroups;
    private final HydrantGroupAssembler hydrantGroups;

    public DeckMapController(DeckMapAssemblyConfig assemblyConfig, DeckMapConfig deckMapConfig,
        TopologyModel topology, ImageIcon mapImage, ActionListener actionListener)
    {
        this.fireIncidentState = new FireIncidentState();
        this.phaseVisibility = new MapLayerVisibilityManager();

        DeckMapGeometryConfig geometryConfig = deckMapConfig.getGeometryConfig();
        DeckMapControlsConfig controlConfig = deckMapConfig.getControlConfig();

        DeckMapGeometry geometry = new DeckMapGeometry(geometryConfig, controlConfig.explosionPreventionMarkers());
        this.mapPainter = new MapPainter(mapImage, geometry, fireIncidentState, FIRE_LINE_WIDTH);
        fireIncidentState.addMapDrawingListener(mapPainter);

        JLabel mapLabel = mapPainter.getUpperLayer();
        this.inputGroups = InputGroupAssembler.assemble(assemblyConfig, deckMapConfig, topology,
            actionListener, fireIncidentState, phaseVisibility);
        this.hydrantGroups = HydrantGroupAssembler.assemble(assemblyConfig, deckMapConfig, topology,
            mapLabel, fireIncidentState, phaseVisibility);

        // Ordered addToMap for every group-layers entry: needs both assemblers' static registries
        // (input controls + hydrant labels). Dynamic hydrant buttons are not in group-layers.
        Map<GroupKey, StaticallyVisible> staticallyVisible = new EnumMap<>(GroupKey.class);
        staticallyVisible.putAll(inputGroups.staticallyVisibleGroups());
        staticallyVisible.putAll(hydrantGroups.staticallyVisibleGroups());
        for (GroupLayerSpec layer : assemblyConfig.groupLayerSpecs())
            addStaticallyVisible(layer, staticallyVisible, mapLabel);
    }

    private static void addStaticallyVisible(GroupLayerSpec layer, Map<GroupKey, StaticallyVisible> staticallyVisible,
        JLabel mapLabel)
    {
        StaticallyVisible group = staticallyVisible.get(layer.key());
        if (group == null)
            throw new IllegalStateException(layer.key() + " group is declared in configuration but implementation not found");
        group.addToMap(mapLabel, layer.initialVisibility());
    }

    @Override
    public void onPhaseChanged(SolutionTreeSection newPhase) {
        phaseVisibility.apply(newPhase);
    }

    public JPanel getMapContainer() {
        return mapPainter.getRootContainer();
    }

    public void repaint() {
        mapPainter.repaint();
    }

    //================================================================
    // CLIPS EVENT HANDLING (collecting changes / representing fire state)
    //================================================================

    public void representFire(FireIncidentSnapshot state) {
        fireIncidentState.updateState(
            buildPaintingViewData(state),
            buildHydrantViewData(state),
            buildInputControlsData(state),
            state.explosionThreatLocations()
        );
    }

    public void repaintExplosionLocations(Set<Location> locations) {
        fireIncidentState.setExplosionThreatLocations(locations);
    }

    public Map<Location, EvacuationStatus> collectEvacChanges(Location location) {
        EvacuationButtonGroup group = inputGroups.getEvacuationGroup();
        requireGroupConfigured(group, GroupKey.EVACUATION_GROUP);
        return group.collectChanges(location);
    }

    public Map<Location, VentilationAction> collectVentChanges(Location location) {
        VentilationButtonGroup group = inputGroups.getVentilationGroup();
        requireGroupConfigured(group, GroupKey.VENTILATION_GROUP);
        return group.collectChanges(location);
    }

    public Map<Link, DoorState> collectDoorChanges(Link door) {
        DoorSealingButtonGroup group = inputGroups.getDoorSealingGroup();
        requireGroupConfigured(group, GroupKey.DOOR_SEALING_GROUP);
        return group.collectChanges(door);
    }

    public Map<Location, ExplosiveType> collectExplosionChanges(Location location, ExplosiveType explosiveType) {
        ExplosionButtonGroup group = inputGroups.getExplosionGroup();
        requireGroupConfigured(group, GroupKey.EXPLOSION_GROUP);
        return group.collectChanges(location, explosiveType);
    }

    public Map<Location, PreventionType> collectFlammableChanges(PreventionType preventionType, Location location) {
        FlammableButtonGroup group = inputGroups.getFlammableGroup();
        requireGroupConfigured(group, GroupKey.FLAMMABLE_GROUP);
        return group.collectChanges(location, preventionType);
    }

    public Map<Location, MachineryDamageAction> collectMachineryDamageChanges(Location location) {
        MachineryDamageButtonGroup group = inputGroups.getMachineryDamageGroup();
        requireGroupConfigured(group, GroupKey.MACHINERY_DAMAGE_GROUP);
        return group.collectChanges(location);
    }

    /**
     * Unlike the other {@code collect*Changes} methods, a missing group here is not a caller
     * error — the extinguisher group is legitimately absent whenever this controller's assembly
     * config doesn't declare {@code EXTINGUISHERS_GROUP} (true of every real shipped config today,
     * see {@link ExtinguisherButtonGroup}'s javadoc), so this returns an empty map instead of
     * throwing, preserving the behavior from before groups became optional.
     */
    public Map<Extinguisher, ExtinguisherUsage> collectExtinguisherChanges(Extinguisher extinguisher) {
        ExtinguisherButtonGroup group = inputGroups.getExtinguisherGroup();
        return group == null ? Map.of() : group.collectChanges(extinguisher);
    }

    private static PaintingViewData buildPaintingViewData(FireIncidentSnapshot state) {
        return new PaintingViewData(state.fireLocations(), state.threatenedLocations(), state.fireLineLinks());
    }

    private static InputControlsData buildInputControlsData(FireIncidentSnapshot state) {
        return new InputControlsData(state.evacuationLocations(), state.ventilationOffLocations(),
            state.sealingDoorsToClose(), state.sealingDoorsKeepOpen(), state.flammableLocations(),
            state.machineryDamageLocations()
        );
    }

    private static HydrantViewData buildHydrantViewData(FireIncidentSnapshot state) {
        return new HydrantViewData(state.frontlineHydrantsBalance(), state.hydrantOutletsState(),
            state.firefightingPlanSteps(), state.fireLineLocations(), state.fireLineHydrantOutletsByLocation(),
            state.fireLocations(), state.extByLocation(), state.extBToByLocation(),
            state.graphFromLocations(), state.extBFromByLocation()
        );
    }

    /**
     * Fails fast with a clear message if the caller asks for a group this controller's assembly
     * config didn't declare — a programming error (wrong controller/wrong test setup), not a
     * legitimate empty state, unlike {@link #collectExtinguisherChanges}.
     */
    private static void requireGroupConfigured(Object group, GroupKey key) {
        if (group == null)
            throw new IllegalStateException(key + " group is not configured for this controller");
    }

    //================================================================
    // TEST-ONLY ACCESSORS
    //================================================================

    /**
     * Input button groups (evac, door, fire, …) — tests reach a group via typed getters
     * (e.g. {@code inputGroups().getEvacuationGroup()}).
     */
    @VisibleForTesting
    InputGroupAssembler inputGroups() {
        return inputGroups;
    }

    /**
     * Hydrant label/button groups — tests reach a group via typed getters
     * (e.g. {@code hydrantGroups().getFireHoseButtonGroup()}).
     */
    @VisibleForTesting
    HydrantGroupAssembler hydrantGroups() {
        return hydrantGroups;
    }

    /**
     * The locations {@link gui.map.input.ExplosionButtonGroup} currently considers prevented (its own
     * button state, pushed via {@code collectChanges}'s {@code preventedLocationsStorage} side
     * effect) — see {@link FireIncidentState#setPreventedExplosionLocations}'s javadoc for why this
     * is tracked separately from CLIPS's own {@code explosionThreatLocations}.
     */
    @VisibleForTesting
    Set<Location> getPreventedExplosionLocations() {
        return fireIncidentState.getPreventedExplosionLocations();
    }
}
