package fixtures;

import clips.ClipsReadOnlyService;
import domain.Explanation;
import domain.Extinguisher;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reusable fake implementation of {@link ClipsReadOnlyService} for controller and
 * presentation-layer tests.
 * <p>
 * Provides controllable return values for the collect/get methods exercised by
 * {@link gui.solution.SolutionResultsController} (evacuation, ventilation/sealing,
 * firefighting plans, extinguishers, fire locations, etc.). All other methods
 * return empty collections so that only the exercised paths need setup in a given
 * test — except the six {@code getExplanationFor*} methods, which return a fixed,
 * per-method-distinguishable {@link Explanation} rather than {@link Explanation#EMPTY}
 * (see their own comment) so a routing test can tell which one a given call reached.
 * <p>
 * Use the fluent builder API:
 * <pre>
 * FakeClipsReadOnlyService clips = FakeClipsReadOnlyService.fakeClips()
 *     .withEvacuation(EVACUATE)
 *     .withVentilationOff(VENTILATION_OFF)
 *     .withFireLocations(fireRooms)
 *     .withExtinguishers(extMap)
 *     .withFirefightingPlan(plan);
 * </pre>
 */
public final class FakeClipsReadOnlyService implements ClipsReadOnlyService {
    private Set<Location> evacuate = Set.of();
    private Set<Location> ventilationOff = Set.of();
    private Set<Location> fireLocations = Set.of();
    private Map<Location, List<Extinguisher>> extinguishersByLocation = Map.of();
    private Map<Location, Location> fireExtinguishingPlanData = Map.of();
    private List<Link> sealingDoorsToClose = List.of();
    private List<Link> sealingDoorsKeepOpen = List.of();
    private Set<Location> isolationPhase = Set.of();
    private Set<Location> explosionPhase = Set.of();
    private Set<Location> machineryDamage = Set.of();
    private Set<Location> fireLineLocations = Set.of();
    private Map<Location, List<HydrantOutlets>> hydrantOutletsByLocation = Map.of();
    private Map<String, Location> stepFromByCode = Map.of();

    public static FakeClipsReadOnlyService fakeClips() {
        return new FakeClipsReadOnlyService();
    }

    public FakeClipsReadOnlyService withEvacuation(Set<Location> evacuate) {
        this.evacuate = (evacuate != null) ? evacuate : Set.of();
        return this;
    }

    public FakeClipsReadOnlyService withVentilationOff(Set<Location> ventilationOff) {
        this.ventilationOff = (ventilationOff != null) ? ventilationOff : Set.of();
        return this;
    }

    public FakeClipsReadOnlyService withFireLocations(Set<Location> fireLocations) {
        this.fireLocations = (fireLocations != null) ? fireLocations : Set.of();
        return this;
    }

    public FakeClipsReadOnlyService withExtinguishers(Map<Location, List<Extinguisher>> extinguishersByLocation) {
        this.extinguishersByLocation = (extinguishersByLocation != null) ? extinguishersByLocation : Map.of();
        return this;
    }

    public FakeClipsReadOnlyService withFirefightingPlan(Map<Location, Location> fireExtinguishingPlanData) {
        this.fireExtinguishingPlanData = (fireExtinguishingPlanData != null) ? fireExtinguishingPlanData : Map.of();
        return this;
    }

    public FakeClipsReadOnlyService withSealingDoorsToClose(List<Link> sealingDoorsToClose) {
        this.sealingDoorsToClose = (sealingDoorsToClose != null) ? sealingDoorsToClose : List.of();
        return this;
    }

    public FakeClipsReadOnlyService withSealingDoorsKeepOpen(List<Link> sealingDoorsKeepOpen) {
        this.sealingDoorsKeepOpen = (sealingDoorsKeepOpen != null) ? sealingDoorsKeepOpen : List.of();
        return this;
    }

    /** Backs {@code collectActionPhase("isolation")} — flammable-material prevention rooms. */
    public FakeClipsReadOnlyService withIsolationPhase(Set<Location> isolationPhase) {
        this.isolationPhase = (isolationPhase != null) ? isolationPhase : Set.of();
        return this;
    }

    /** Backs {@code collectActionPhase("explosion")} — explosion-prevention rooms. */
    public FakeClipsReadOnlyService withExplosionPhase(Set<Location> explosionPhase) {
        this.explosionPhase = (explosionPhase != null) ? explosionPhase : Set.of();
        return this;
    }

    public FakeClipsReadOnlyService withMachineryDamage(Set<Location> machineryDamage) {
        this.machineryDamage = (machineryDamage != null) ? machineryDamage : Set.of();
        return this;
    }

    public FakeClipsReadOnlyService withFireLineLocations(Set<Location> fireLineLocations) {
        this.fireLineLocations = (fireLineLocations != null) ? fireLineLocations : Set.of();
        return this;
    }

    public FakeClipsReadOnlyService withHydrantOutletsByLocation(Map<Location, List<HydrantOutlets>> hydrantOutletsByLocation) {
        this.hydrantOutletsByLocation = (hydrantOutletsByLocation != null) ? hydrantOutletsByLocation : Map.of();
        return this;
    }

    public FakeClipsReadOnlyService withStepFrom(Map<String, Location> stepFromByCode) {
        this.stepFromByCode = (stepFromByCode != null) ? stepFromByCode : Map.of();
        return this;
    }

    @Override
    public Set<Location> collectEvacuationLocations(String status) {
        return evacuate;
    }

    @Override
    public Set<Location> collectSealingLocations(String action) {
        return ventilationOff;
    }

    @Override
    public List<Link> collectSealingDoors(String action) {
        // The real service returns two distinct lists keyed by this action string (see
        // ExpertSystemService#collectSealingDoors → clips.collectDoorsToSeal(action)); mirror that
        // so a test can drive the to-close and keep-open door-row paths independently.
        return "keep-open".equals(action) ? sealingDoorsKeepOpen : sealingDoorsToClose;
    }

    @Override
    public List<HydrantOutlets> getHydrantOutletsForLocation(Location location) {
        return hydrantOutletsByLocation.getOrDefault(location, List.of());
    }

    @Override
    public Map<Location, Location> getFirefightingPlanPairs() {
        return fireExtinguishingPlanData;
    }

    @Override
    public Set<Location> collectActionPhase(String phase) {
        return "explosion".equals(phase) ? explosionPhase : isolationPhase;
    }

    @Override
    public Set<Location> collectMachineryDamageLocations(String action) {
        return machineryDamage;
    }

    @Override
    public Set<Location> getFireLineLocations() {
        return fireLineLocations;
    }

    @Override
    public Location getStepFrom(String locationCode) {
        return stepFromByCode.getOrDefault(locationCode, null);
    }

    // Each returns a fixed literal tagged with its own method name rather than Explanation.EMPTY —
    // distinguishable from one another so a routing test (asserting which of these six a given
    // actions-table row resolved to) can tell them apart; none is settable, since no test needs a
    // *different* value per case, only to see which method got called.

    @Override
    public Explanation getExplanationForEvacuation(Location location) {
        return new Explanation("evacuation-antec1", "evacuation-antec2", "evacuation-consec");
    }

    @Override
    public Explanation getExplanationForLocation(Location location) {
        return new Explanation("location-antec1", "location-antec2", "location-consec");
    }

    @Override
    public Explanation getExplanationForDoorSealing(Link link) {
        return new Explanation("door-sealing-antec1", "door-sealing-antec2", "door-sealing-consec");
    }

    @Override
    public Explanation getExplanationForExplosions(Location location) {
        return new Explanation("explosions-antec1", "explosions-antec2", "explosions-consec");
    }

    @Override
    public Explanation getExplanationForFlammable(Location location) {
        return new Explanation("flammable-antec1", "flammable-antec2", "flammable-consec");
    }

    @Override
    public Explanation getExplanationForMachineryDamage(Location location) {
        return new Explanation("machinery-damage-antec1", "machinery-damage-antec2", "machinery-damage-consec");
    }

    @Override
    public Set<Location> getFireLocations() {
        return fireLocations;
    }

    @Override
    public List<Extinguisher> getExtinguishersForLocation(Location location) {
        return extinguishersByLocation.getOrDefault(location, List.of());
    }
}
