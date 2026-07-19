package clips;

import domain.Explanation;
import domain.Extinguisher;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read / query side of the CLIPS API: inspect engine state without reporting operator actions. Implemented by
 * {@link ExpertSystemService}; consumed primarily by {@code gui.solution} (actions table, explanations). Contrast with
 * {@link ClipsReportService} (write / command).
 * <p>
 * Several collectors take a raw CLIPS status / phase string — those tokens are the rule base's own vocabulary (not Java
 * enums). Callers must pass the exact symbols the corresponding {@code collect-*}/{@code get-*} functions in
 * {@code feis.clp} expect.
 */
public interface ClipsReadOnlyService {

    /**
     * Hydrant outlets assigned to the given fire-line location ({@code get-hydr-for-location}).
     */
    List<HydrantOutlets> getHydrantOutletsForLocation(Location location);

    /**
     * Firefighting plan edges: each fire room → the previous location on its extinguishing route.
     */
    Map<Location, Location> getFirefightingPlanPairs();

    /**
     * Locations matching an evacuation status token (e.g. {@code to-evacuate}).
     */
    Set<Location> collectEvacuationLocations(String status);

    /**
     * Locations matching a ventilation / sealing action token (e.g. {@code to-off}). Despite the name, this is the
     * ventilation-location collector — not doors.
     */
    Set<Location> collectSealingLocations(String action);

    /**
     * Doors matching a germetisation status token (e.g. {@code to-close}, {@code keep-open}).
     */
    List<Link> collectSealingDoors(String action);

    /**
     * Locations in a prevention action phase (e.g. {@code explosion}, {@code isolation}).
     */
    Set<Location> collectActionPhase(String phase);

    /**
     * Locations matching a machinery-damage status token (e.g. {@code stop}).
     */
    Set<Location> collectMachineryDamageLocations(String action);

    Explanation getExplanationForEvacuation(Location location);

    Explanation getExplanationForLocation(Location location);

    Explanation getExplanationForDoorSealing(Link link);

    Explanation getExplanationForExplosions(Location location);

    Explanation getExplanationForFlammable(Location location);

    Explanation getExplanationForMachineryDamage(Location location);

    Set<Location> getFireLineLocations();

    /**
     * Previous location on the firefighting plan for {@code locationCode}, or {@code null} if none.
     */
    Location getStepFrom(String locationCode);

    /**
     * Locations currently reporting an active fire (CLIPS status {@code fire} on slot {@code accedent} — the rule
     * base's historical spelling).
     */
    Set<Location> getFireLocations();

    /**
     * Unused portable extinguishers ({@code used no}) available at the given location.
     */
    List<Extinguisher> getExtinguishersForLocation(Location location);
}
