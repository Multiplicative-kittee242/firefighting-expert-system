package clips;

import domain.FirefightingStep;
import domain.FrontlineHydrantsBalance;
import domain.HydrantState;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;

import java.util.*;

/**
 * Immutable snapshot of the system state immediately after reporting a fire accident — the single DTO
 * {@link ExpertSystemService#reportFireIncident} returns for the map and tables.
 * <p>
 * Field groups follow decision-support phases (evacuation, sealing, localization, prevention, firefighting). Maps such
 * as {@code extByLocation}/{@code extB*ByLocation}/{@code graphFromLocations} are fully wired but currently always
 * empty at runtime (abandoned border-routed hydrant assignment in the rule base; see {@code clips/INACTIVE.md}).
 */
public record FireIncidentSnapshot(
    Set<Location> fireLocations, Set<Location> threatenedLocations,
    Set<Location> evacuationLocations, Set<Location> ventilationOffLocations,
    List<Link> sealingDoorsToClose, List<Link> sealingDoorsKeepOpen,
    Set<Location> explosionThreatLocations, Set<Location> flammableLocations,
    Set<Location> machineryDamageLocations, List<Link> fireLineLinks,
    Set<Location> fireLineLocations, Set<Location> graphFromLocations,
    Map<Location, FrontlineHydrantsBalance> frontlineHydrantsBalance,
    Map<HydrantOutlets, HydrantState> hydrantOutletsState,
    Map<Location, FirefightingStep> firefightingPlanSteps,
    Map<Location, List<HydrantOutlets>> extBToByLocation,
    Map<Location, List<HydrantOutlets>> extByLocation,
    Map<Location, List<HydrantOutlets>> extBFromByLocation,
    Map<Location, List<HydrantOutlets>> fireLineHydrantOutletsByLocation)
{
    public FireIncidentSnapshot(
        Set<Location> fireLocations,
        Set<Location> threatenedLocations,
        Set<Location> evacuationLocations,
        Set<Location> ventilationOffLocations,
        List<Link> sealingDoorsToClose,
        List<Link> sealingDoorsKeepOpen,
        Set<Location> explosionThreatLocations,
        Set<Location> flammableLocations,
        Set<Location> machineryDamageLocations,
        List<Link> fireLineLinks,
        Set<Location> fireLineLocations,
        Set<Location> graphFromLocations,
        Map<Location, FrontlineHydrantsBalance> frontlineHydrantsBalance,
        Map<HydrantOutlets, HydrantState> hydrantOutletsState,
        Map<Location, FirefightingStep> firefightingPlanSteps,
        Map<Location, List<HydrantOutlets>> extBToByLocation,
        Map<Location, List<HydrantOutlets>> extByLocation,
        Map<Location, List<HydrantOutlets>> extBFromByLocation,
        Map<Location, List<HydrantOutlets>> fireLineHydrantOutletsByLocation
    ) {
        // --- Parsed single-location collections (defensive copies) ---
        this.fireLocations = copySet(fireLocations);
        this.threatenedLocations = copySet(threatenedLocations);
        this.evacuationLocations = copySet(evacuationLocations);
        this.ventilationOffLocations = copySet(ventilationOffLocations);
        this.explosionThreatLocations = copySet(explosionThreatLocations);
        this.flammableLocations = copySet(flammableLocations);
        this.machineryDamageLocations = copySet(machineryDamageLocations);
        this.fireLineLocations = copySet(fireLineLocations);
        this.graphFromLocations = copySet(graphFromLocations);

        // --- Parsed location-keyed maps (defensive copies) ---
        this.frontlineHydrantsBalance = copyLocationValueMap(frontlineHydrantsBalance);
        this.firefightingPlanSteps = copyLocationValueMap(firefightingPlanSteps);
        this.extBToByLocation = copyLocationHydrantListMap(extBToByLocation);
        this.extByLocation = copyLocationHydrantListMap(extByLocation);
        this.extBFromByLocation = copyLocationHydrantListMap(extBFromByLocation);
        this.fireLineHydrantOutletsByLocation = copyLocationHydrantListMap(fireLineHydrantOutletsByLocation);

        // --- Hydrant outlets: typed keys, typed CLIPS state values ---
        this.hydrantOutletsState = copyHydrantOutletsMap(hydrantOutletsState);

        // --- Doors (defensive copies) ---
        this.sealingDoorsToClose = copyLinkList(sealingDoorsToClose);
        this.sealingDoorsKeepOpen = copyLinkList(sealingDoorsKeepOpen);
        // --- Fire-line links: compartment-border pairs, not all of which are doors ---
        this.fireLineLinks = copyLinkList(fireLineLinks);
    }

    // ==================== PARSING HELPERS ====================

    private static List<Link> copyLinkList(List<Link> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyList();

        return List.copyOf(source);
    }

    private static Map<HydrantOutlets, HydrantState> copyHydrantOutletsMap(Map<HydrantOutlets, HydrantState> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyMap();

        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static Set<Location> copySet(Set<Location> source) {
        if (source == null || source.isEmpty())
            return Collections.emptySet();

        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    private static <V> Map<Location, V> copyLocationValueMap(Map<Location, V> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyMap();

        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static Map<Location, List<HydrantOutlets>> copyLocationHydrantListMap(Map<Location, List<HydrantOutlets>> source) {
        if (source == null || source.isEmpty())
            return Collections.emptyMap();

        Map<Location, List<HydrantOutlets>> copy = new LinkedHashMap<>();
        for (Map.Entry<Location, List<HydrantOutlets>> entry : source.entrySet())
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        return Collections.unmodifiableMap(copy);
    }
}
