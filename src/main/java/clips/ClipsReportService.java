package clips;

import clips.values.*;
import clips.values.internal.ExplosionClipsAction;
import clips.values.internal.ExtinguisherClipsStatus;
import clips.values.internal.FlammablePreventionClipsAction;
import domain.Extinguisher;
import domain.Link;
import domain.Location;

import java.util.Map;
import java.util.Set;

/**
 * Service interface for reporting user actions and events back into the CLIPS expert system.
 * <p>
 * Contains all methods that modify the internal state of the expert system as a result of user interaction in the Swing
 * UI (pressing buttons, confirming evacuation, sealing, flammable-material prevention, etc.).
 * <p>
 * This interface represents the **write / command** side of communication with CLIPS, in contrast to
 * {@link ClipsReadOnlyService}, which is responsible for reading state.
 */
public interface ClipsReportService {

    /**
     * Reports a fire accident at the specified location and runs the full inference cycle of the expert system.
     * <p>
     * This is the main entry point that triggers all phases of decision support (evacuation, sealing, prevention,
     * localization, extinguishing plan).
     *
     * @param location location of the fire accident @return immutable snapshot of the system state after inference
     */
    FireIncidentSnapshot reportFireIncident(Location location);

    /**
     * Reports changes in evacuation status of compartments.
     *
     * @param changes map of location → new evacuation status
     */
    void reportEvacuationChanges(Map<Location, EvacuationStatus> changes);

    /**
     * Reports changes in ventilation status of compartments.
     *
     * @param changes map of location → new ventilation status
     */
    void reportVentilationChanges(Map<Location, VentilationAction> changes);

    /**
     * Reports door open / close state after the operator toggles a sealing control. Only
     * {@link DoorState#OPEN}/{@link DoorState#CLOSE} are sent ({@code open}/{@code close} in CLIPS). Recommendation
     * labels such as {@code to-close}/{@code keep-open} are <em>query</em> statuses from the read path, not values of
     * this map; keep-open paint state on the map never enters a report.
     *
     * @param changes map of door link → new open / close state
     */
    void reportDoorSealingChanges(Map<Link, DoorState> changes);

    /**
     * Reports user actions related to explosion prevention and returns the updated set of locations still pending an
     * explosion-prevention action.
     *
     * @param changes map of location → action status @return locations still pending explosion prevention, for
     * repainting
     */
    Set<Location> reportExplosionPreventionChanges(Map<Location, ExplosionClipsAction> changes);

    /**
     * Reports changes related to machinery damage prevention.
     *
     * @param changes map of location → new machinery status
     */
    void reportMachineryDamagePreventionChanges(Map<Location, MachineryDamageAction> changes);

    /**
     * Reports changes related to flammable materials ignition prevention.
     *
     * @param changes map of location → new flammable-prevention status
     */
    void reportFlammablePreventionChanges(Map<Location, FlammablePreventionClipsAction> changes);

    /**
     * Reports a portable extinguisher as used or not used. No user-facing button is placed on the map yet (see
     * {@code gui.map.input.ExtinguisherButtonGroup}), so this path is currently unreachable from the GUI — kept ready
     * for when placement is introduced.
     *
     * @param changes map of extinguisher → new used status
     */
    void reportExtinguisherChanges(Map<Extinguisher, ExtinguisherClipsStatus> changes);
}
