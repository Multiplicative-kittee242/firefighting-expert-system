package clips;

import clips.values.*;
import clips.values.internal.ExplosionClipsAction;
import clips.values.internal.ExtinguisherClipsStatus;
import clips.values.internal.FlammablePreventionClipsAction;
import domain.*;
import domain.registry.TopologyModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * High-level service for interacting with the CLIPS expert system.
 * <p>
 * This class contains business logic, orchestration of firefighting plans, and construction of
 * {@link FireIncidentSnapshot} — entirely in domain terms. It delegates all direct CLIPS engine interaction, including
 * the string↔domain translation in both directions, to {@link ClipsEngineAccess}.
 */
public class ExpertSystemService implements ClipsReportService, ClipsReadOnlyService {
    private static final String CLIPS_RULES_BASE = "clips/feis.clp";
    private static final String EXPLANATION_ANTEC1 = "antec1";
    private static final String EXPLANATION_ANTEC2 = "antec2";
    private static final String EXPLANATION_CONSEC = "consec";

    private final ClipsEngineAccess clips;
    private final List<HydrantOutlets> existingHydrantOutlets;

    public ExpertSystemService(TopologyModel topology) {
        this.clips = new ClipsEngineAccess(CLIPS_RULES_BASE, topology);
        this.clips.initializeTopology();
        this.existingHydrantOutlets = topology.allHydrantOutlets();
    }

    /**
     * Reuses this instance's single native CLIPS {@code Environment} for a new, independent scenario instead of
     * constructing a whole new {@code ExpertSystemService} (which would mean a new native {@code Environment}).
     * Constructing and destroying many short-lived {@code Environment} s in one JVM process was found to reliably crash
     * the JVM ({@code EXCEPTION_ACCESS_VIOLATION} inside CLIPSJNI.dll) — {@code CLIPSJNI.Environment} has no public
     * dispose API, so teardown is entirely GC-finalizer-driven, and finalizing several native environments in the same
     * process races (see {@code IncidentReportLoopIntegrationTest}'s class javadoc for the full history). Reusing one
     * environment and resetting it between scenarios (CLIPS's own {@code (reset)}, which clears facts / instances but
     * keeps the loaded rule base — confirmed in {@code ClipsEnvironmentLifecycleTest}) sidesteps the problem entirely:
     * only one {@code Environment} is ever constructed for the instance's lifetime.
     * <p>
     * Not used by the running application (a live session only ever reports one incident) — only by tests that exercise
     * many independent scenarios in one process. Callers must not run two scenarios against the same instance
     * concurrently: the shared {@code Environment} is not thread-safe, and even sequential test execution must not be
     * reordered onto multiple threads (see the {@code @Execution}/{@code @ResourceLock} annotations on this class's
     * test callers).
     */
    public void resetForNewScenario() {
        clips.reset();
        clips.initializeTopology();
    }

    /**
     * Reports a fire accident at the given location and returns the full resulting state. This is the single entry
     * point for handling sensor / button fire events.
     */
    public FireIncidentSnapshot reportFireIncident(Location accidentLocation) {
        clips.executeIncident(accidentLocation, "fire");

        Set<Location> fireLocations = getFireLocations();
        Set<Location> threatenedLocations = clips.collectLocationsByStatus("threat");
        Set<Location> evacuationLocations = clips.collectEvacuationLocations("to-evacuate");
        Set<Location> ventilationOffLocations = clips.collectVentilationLocations("to-off");
        List<Link> sealingDoorsToClose = clips.collectDoorsToSeal("to-close");
        List<Link> sealingDoorsKeepOpen = clips.collectDoorsToSeal("keep-open");
        Set<Location> explosionPreventionLocations = clips.collectActionPhase("explosion");
        Set<Location> flammableLocations = clips.collectActionPhase("isolation");
        Set<Location> machineryDamageLocations = clips.collectMachineryDamageLocations("stop");
        List<Link> fireLineBorders = clips.getFrontLineBorders();
        Set<Location> fireLineLocations = clips.getFireLineLocations();
        Set<Location> graphFromLocations = clips.getGraphFromLocations();

        Map<Location, FrontlineHydrantsBalance> frontlineHydrants = new LinkedHashMap<>();
        for (Location location : fireLineLocations) {
            int here = clips.getFireLineHydrantsPresent(location.getCode());
            int need = clips.getFireLineHydrantsNeeded(location.getCode());
            frontlineHydrants.put(location, new FrontlineHydrantsBalance(here, need));
        }

        Map<Location, FirefightingStep> firefightingSteps = new LinkedHashMap<>();
        Map<Location, List<HydrantOutlets>> extBToByLocation = new LinkedHashMap<>();
        Map<Location, List<HydrantOutlets>> extByLocation = new LinkedHashMap<>();
        for (Location location : fireLocations) {
            // План тушения для комнаты: предыдущая локация маршрута + номер шага плана из CLIPS.
            // Плана может ещё не быть (например, для самого очага возгорания, пока не найден путь
            // от соседней угрожаемой зоны) — тогда запись в карту просто не добавляется.
            Location from = clips.getStepFrom(location.getCode());
            if (from != null) {
                int stepNumber = clips.getStepNumber(location.getCode());
                firefightingSteps.put(location, new FirefightingStep(from, stepNumber));
            }
            extBToByLocation.put(location, clips.getExtBToForLocation(location.getCode()));
            extByLocation.put(location, clips.getExtForLocation(location.getCode()));
        }

        Map<Location, List<HydrantOutlets>> fireLineHydrantOutletsByLocation = new LinkedHashMap<>();
        for (Location location : fireLineLocations)
            fireLineHydrantOutletsByLocation.put(location, clips.getHydrantsForLocation(location.getCode()));

        Map<Location, List<HydrantOutlets>> extBFromByLocation = new LinkedHashMap<>();
        for (Location location : graphFromLocations)
            extBFromByLocation.put(location, clips.getExtBFromForLocation(location.getCode()));

        Map<HydrantOutlets, HydrantState> hydrantOutsByTitle = new LinkedHashMap<>();
        for (HydrantOutlets hydrant : existingHydrantOutlets) {
            String title = hydrant.getTitle();
            int currentFree = clips.getHydrantFreeOutlets(title);
            int totalOutlets = clips.getHydrantTotalOutlets(title);
            hydrantOutsByTitle.put(hydrant, new HydrantState(hydrant, totalOutlets, currentFree));
        }

        return new FireIncidentSnapshot(fireLocations, threatenedLocations, evacuationLocations, ventilationOffLocations,
                sealingDoorsToClose, sealingDoorsKeepOpen, explosionPreventionLocations, flammableLocations, machineryDamageLocations,
                fireLineBorders, fireLineLocations, graphFromLocations, frontlineHydrants, hydrantOutsByTitle,
                firefightingSteps, extBToByLocation, extByLocation, extBFromByLocation,
                fireLineHydrantOutletsByLocation);
    }

    // ==================== PUBLIC DELEGATING METHODS (simple CLIPS wrappers) ====================

    @Override
    public List<HydrantOutlets> getHydrantOutletsForLocation(Location location) {
        return clips.getHydrantsForLocation(location.getCode());
    }

    @Override
    public Explanation getExplanationForEvacuation(Location location) {
        if (location == null) return Explanation.EMPTY;
        String code = location.getCode();
        String previous1 = clips.getExplanationEvacuation(EXPLANATION_ANTEC1, code);
        String previous2 = clips.getExplanationEvacuation(EXPLANATION_ANTEC2, code);
        String consequent = clips.getExplanationEvacuation(EXPLANATION_CONSEC, code);
        return new Explanation(previous1, previous2, consequent);
    }

    @Override
    public Explanation getExplanationForLocation(Location location) {
        if (location == null) return Explanation.EMPTY;
        String code = location.getCode();
        String previous1 = clips.getExplanation(EXPLANATION_ANTEC1, code);
        String previous2 = clips.getExplanation(EXPLANATION_ANTEC2, code);
        String consequent = clips.getExplanation(EXPLANATION_CONSEC, code);
        return new Explanation(previous1, previous2, consequent);
    }

    @Override
    public Explanation getExplanationForDoorSealing(Link link) {
        if (link == null) return Explanation.EMPTY;
        String fromCode = link.getFrom().getCode();
        String toCode = link.getTo().getCode();
        String previous1 = clips.getExplanation(EXPLANATION_ANTEC1, fromCode, toCode);
        String previous2 = clips.getExplanation(EXPLANATION_ANTEC2, fromCode, toCode);
        String consequent = clips.getExplanation(EXPLANATION_CONSEC, fromCode, toCode);
        return new Explanation(previous1, previous2, consequent);
    }

    @Override
    public Explanation getExplanationForExplosions(Location location) {
        if (location == null) return Explanation.EMPTY;
        String code = location.getCode();
        String previous1 = clips.getExplanationForExplosions(EXPLANATION_ANTEC1, code);
        String previous2 = clips.getExplanationForExplosions(EXPLANATION_ANTEC2, code);
        String consequent = clips.getExplanationForExplosions(EXPLANATION_CONSEC, code);
        return new Explanation(previous1, previous2, consequent);
    }

    @Override
    public Explanation getExplanationForFlammable(Location location) {
        if (location == null) return Explanation.EMPTY;
        String code = location.getCode();
        String previous1 = clips.getExplanationForFlammable(EXPLANATION_ANTEC1, code);
        String previous2 = clips.getExplanationForFlammable(EXPLANATION_ANTEC2, code);
        String consequent = clips.getExplanationForFlammable(EXPLANATION_CONSEC, code);
        return new Explanation(previous1, previous2, consequent);
    }

    @Override
    public Explanation getExplanationForMachineryDamage(Location location) {
        if (location == null) return Explanation.EMPTY;
        String code = location.getCode();
        String previous1 = clips.getExplanationForMachineryDamage(EXPLANATION_ANTEC1, code);
        String previous2 = clips.getExplanationForMachineryDamage(EXPLANATION_ANTEC2, code);
        String consequent = clips.getExplanationForMachineryDamage(EXPLANATION_CONSEC, code);
        return new Explanation(previous1, previous2, consequent);
    }

    @Override
    public Map<Location, Location> getFirefightingPlanPairs() {
        Set<Location> fireRooms = clips.collectLocationsByStatus("fire");
        Map<Location, Location> roomToFromLocation = new LinkedHashMap<>();
        for (Location room : fireRooms)
            roomToFromLocation.put(room, clips.getStepFrom(room.getCode()));
        return roomToFromLocation;
    }

    @Override
    public Set<Location> collectEvacuationLocations(String status) {
        return clips.collectEvacuationLocations(status);
    }

    @Override
    public Set<Location> collectSealingLocations(String action) {
        return clips.collectVentilationLocations(action);
    }

    @Override
    public List<Link> collectSealingDoors(String action) {
        return clips.collectDoorsToSeal(action);
    }

    @Override
    public Set<Location> collectActionPhase(String phase) {
        return clips.collectActionPhase(phase);
    }

    @Override
    public Set<Location> collectMachineryDamageLocations(String action) {
        return clips.collectMachineryDamageLocations(action);
    }

    @Override
    public Set<Location> getFireLineLocations() {
        return clips.getFireLineLocations();
    }

    @Override
    public Location getStepFrom(String locationCode) {
        return clips.getStepFrom(locationCode);
    }

    @Override
    public Set<Location> getFireLocations() {
        return clips.collectLocationsByStatus("fire");
    }

    @Override
    public List<Extinguisher> getExtinguishersForLocation(Location location) {
        return clips.getExtinguishersForLocation(location.getCode());
    }

    // ==================== HIGH-LEVEL REPORTING (business logic + focus) ====================

    /**
     * Reports changes in evacuation status of compartments, converting each {@link EvacuationStatus} to its CLIPS value
     * before delegating to the engine.
     */
    @Override
    public void reportEvacuationChanges(Map<Location, EvacuationStatus> changes) {
        if (changes == null || changes.isEmpty())
            return;

        clips.executeWithFocus(() -> {
            for (Map.Entry<Location, EvacuationStatus> entry : changes.entrySet())
                clips.reportEvacuation(entry.getKey().getCode(), entry.getValue().getClipsValue());
            return null;
        }, "(focus IMMEDIATE-EVACUATION)");
    }

    /**
     * Reports changes in ventilation status of compartments, converting each {@link VentilationAction} to its CLIPS
     * value before delegating to the engine.
     */
    @Override
    public void reportVentilationChanges(Map<Location, VentilationAction> changes) {
        if (changes == null || changes.isEmpty())
            return;

        clips.executeWithFocus(() -> {
            for (Map.Entry<Location, VentilationAction> entry : changes.entrySet())
                clips.reportVentilation(entry.getKey().getCode(), entry.getValue().getClipsValue());
            return null;
        }, "IMMEDIATE-GERMETISATION");
    }

    /**
     * Reports door open / close state, converting each {@link DoorState} to its CLIPS value (
     * {@code open}/{@code close}) before delegating to the engine. Not the recommendation statuses
     * {@code to-close}/{@code keep-open} from the read path.
     */
    @Override
    public void reportDoorSealingChanges(Map<Link, DoorState> changes) {
        if (changes == null || changes.isEmpty())
            return;

        clips.executeWithFocus(() -> {
            for (Map.Entry<Link, DoorState> entry : changes.entrySet())
                clips.reportDoorStatus(entry.getKey(), entry.getValue().getClipsValue());
            return null;
        }, "IMMEDIATE-GERMETISATION");
    }

    /**
     * Reports user actions related to explosion prevention, converting each {@link ExplosionClipsAction} to its CLIPS
     * value before delegating to the engine, and returns the locations still pending explosion prevention.
     */
    @Override
    public Set<Location> reportExplosionPreventionChanges(Map<Location, ExplosionClipsAction> changes) {
        return clips.executeWithFocus(() -> {
            for (Map.Entry<Location, ExplosionClipsAction> entry : changes.entrySet())
                clips.reportExplosionPrevention(entry.getKey().getCode(), entry.getValue().getClipsValue());
            return clips.collectActionPhase("explosion");
        }, "IMMEDIATE-EXPLOSION");
    }

    /**
     * Reports machinery-damage prevention for each location. {@link MachineryDamageAction} has no string wire token:
     * {@link MachineryDamageAction#DONE} calls {@code reportMachineryDone}, any other value (
     * {@link MachineryDamageAction#STOP}) calls {@code reportMachineryStop}.
     */
    @Override
    public void reportMachineryDamagePreventionChanges(Map<Location, MachineryDamageAction> changes) {
        if (changes == null || changes.isEmpty())
            return;

        clips.executeWithFocus(() -> {
            for (Map.Entry<Location, MachineryDamageAction> entry : changes.entrySet()) {
                String locationCode = entry.getKey().getCode();
                MachineryDamageAction status = entry.getValue();
                if (status == MachineryDamageAction.DONE) {
                    clips.reportMachineryDone(locationCode);
                } else {
                    clips.reportMachineryStop(locationCode);
                }
            }
            return null;
        }, "IMMEDIATE-ISOLATION");
    }

    /**
     * Reports changes related to inflammation prevention of combustible materials, converting each
     * {@link FlammablePreventionClipsAction} to its CLIPS value before delegating to the engine.
     */
    @Override
    public void reportFlammablePreventionChanges(Map<Location, FlammablePreventionClipsAction> changes) {
        if (changes == null || changes.isEmpty())
            return;

        clips.executeWithFocus(() -> {
            for (Map.Entry<Location, FlammablePreventionClipsAction> entry : changes.entrySet())
                clips.reportFlammablePrevention(entry.getKey().getCode(), entry.getValue().getClipsValue());
            return null;
        }, "IMMEDIATE-ISOLATION");
    }

    /**
     * Reports each extinguisher's used status, converting each {@link ExtinguisherClipsStatus} to its CLIPS value
     * before delegating to the engine.
     * <p>
     * Unlike the other reporters above, this deliberately does not wrap the sends in
     * {@link ClipsEngineAccess#executeWithFocus}: {@code used} is read only by this class's own
     * {@link #getExtinguishersForLocation} query and by the {@code used no} guard on
     * {@code IMMEDIATE-EXTINGUISHERS::use-local} — no rule anywhere reacts to it changing. Running that module's agenda
     * here would only risk re-firing that printout rule a second time for the same extinguisher if a user ever un-marks
     * it as used (the guard would newly match again), which is not a behavior this reporter should introduce as a side
     * effect of reporting.
     */
    @Override
    public void reportExtinguisherChanges(Map<Extinguisher, ExtinguisherClipsStatus> changes) {
        if (changes == null || changes.isEmpty())
            return;

        for (Map.Entry<Extinguisher, ExtinguisherClipsStatus> entry : changes.entrySet())
            clips.reportExtinguisherUsed(entry.getKey().getTitle(), entry.getValue().getClipsValue());
    }
}
