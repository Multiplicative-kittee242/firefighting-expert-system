package gui.actions;

import clips.ClipsReportService;
import clips.FireIncidentSnapshot;
import clips.values.DoorState;
import clips.values.EvacuationStatus;
import clips.values.MachineryDamageAction;
import clips.values.VentilationAction;
import clips.values.internal.ExplosionClipsAction;
import clips.values.internal.ExtinguisherClipsStatus;
import clips.values.internal.FlammablePreventionClipsAction;
import domain.Extinguisher;
import domain.Link;
import domain.Location;
import domain.types.PreventionType;
import gui.map.DeckMapController;
import domain.types.ExplosiveType;
import gui.map.state.FireIncidentState;
import gui.map.values.ExtinguisherUsage;
import gui.solution.SolutionPhaseTree;
import gui.solution.SolutionResultsController;

import java.util.Map;
import java.util.Set;

/**
 * Sealed hierarchy of typed UI actions dispatched from map button clicks via
 * {@link ActionDispatcher}, replacing the legacy string {@code actionCommand}
 * parsing that used to live in {@code app.Main}'s {@code actionPerformed} method.
 */
public sealed interface InputAction {
    String INPUT_ACTION_PROPERTY = "inputAction";

    /**
     * Routes this action to the appropriate service calls.
     * <p>
     * Each action is responsible for triggering exactly one overlay repaint. Actions that mutate
     * {@link FireIncidentState} drawing data (fire, explosion prevention) repaint reactively
     * through the state listeners and must not repaint again; the remaining input-only actions carry no
     * reactive path and repaint the map explicitly at the end of {@code apply}.
     * <p>
     * Every action that reports an incremental change to CLIPS must also call
     * {@code solutionTree.refreshCurrentPhase()} afterward (except {@link FireActionInput}, which
     * starts a new incident and calls {@code resetPhaseAndNotify()} instead) — otherwise
     * {@link SolutionResultsController}'s actions table goes stale, since it only re-queries
     * CLIPS when the phase tree notifies it, which otherwise only happens on an actual tree-selection
     * change. This was a long-standing bug (rows never disappearing after the action they describe is
     * completed) before this contract existed — do not drop the call when adding a new action.
     */
    void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
        SolutionPhaseTree solutionTree, SolutionResultsController resultsController);

    /**
     * A fire accident reported at the given location (sensor or fire call-point button).
     */
    record FireActionInput(Location location) implements InputAction {
        @Override
        public void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
            SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
        {
            FireIncidentSnapshot state = clipsReportService.reportFireIncident(location);
            deckMapController.representFire(state);
            resultsController.updateEvents(location);
            solutionTree.resetPhaseAndNotify();
        }
    }

    /**
     * Change of ventilation state in a room. The status (ON / OFF) is determined
     * automatically by collectChanges() in the group.
     */
    record VentilationActionInput(Location location) implements InputAction {
        @Override
        public void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
            SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
        {
            Map<Location, VentilationAction> changes = deckMapController.collectVentChanges(location);
            clipsReportService.reportVentilationChanges(changes);
            deckMapController.repaint();
            solutionTree.refreshCurrentPhase();
        }
    }

    /**
     * Action on a door (close it, or keep it open for hose deployment). The status
     * (OPEN / CLOSE) is determined automatically by collectChanges() in the group.
     */
    record DoorSealingActionInput(Link link) implements InputAction {
        @Override
        public void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
            SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
        {
            Map<Link, DoorState> changes = deckMapController.collectDoorChanges(link);
            clipsReportService.reportDoorSealingChanges(changes);
            deckMapController.repaint();
            solutionTree.refreshCurrentPhase();
        }
    }

    /**
     * Ignition prevention for flammable materials (oil, clothing, etc.). The status
     * (DONE / PUMP_OUT / CARRY_OUT) is determined automatically by collectChanges()
     * at the moment of the click.
     */
    record FlammableActionInput(Location location, PreventionType type) implements InputAction {
        @Override
        public void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
            SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
        {
            Map<Location, PreventionType> changes = deckMapController.collectFlammableChanges(type, location);
            Map<Location, FlammablePreventionClipsAction> remappedChanges = ClipsValuesMapper.remapToClips(changes, ClipsValuesMapper::toClips);
            clipsReportService.reportFlammablePreventionChanges(remappedChanges);
            deckMapController.repaint();
            solutionTree.refreshCurrentPhase();
        }
    }

    /**
     * Prevention of machinery damage (stopping equipment).
     */
    record MachineryDamageActionInput(Location location) implements InputAction {
        @Override
        public void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
            SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
        {
            Map<Location, MachineryDamageAction> changes = deckMapController.collectMachineryDamageChanges(location);
            clipsReportService.reportMachineryDamagePreventionChanges(changes);
            deckMapController.repaint();
            solutionTree.refreshCurrentPhase();
        }
    }

    /**
     * Explosion prevention in a room (air cylinders, diesel fuel, chemical reagent).
     */
    record ExplosionPreventionActionInput(Location location, ExplosiveType type) implements InputAction {
        @Override
        public void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
            SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
        {
            Map<Location, ExplosiveType> changes = deckMapController.collectExplosionChanges(location, type);
            Map<Location, ExplosionClipsAction> remappedChanges = ClipsValuesMapper.remapToClips(changes, ClipsValuesMapper::toClips);
            Set<Location> remaining = clipsReportService.reportExplosionPreventionChanges(remappedChanges);
            deckMapController.repaintExplosionLocations(remaining);
            solutionTree.refreshCurrentPhase();
        }
    }

    /**
     * Evacuation action in a room (enabling/disabling the evacuation route). The
     * status (DONE / NONE) is determined automatically by collectChanges() in the group.
     */
    record EvacuationActionInput(Location location) implements InputAction {
        @Override
        public void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
            SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
        {
            Map<Location, EvacuationStatus> changes = deckMapController.collectEvacChanges(location);
            clipsReportService.reportEvacuationChanges(changes);
            deckMapController.repaint();
            solutionTree.refreshCurrentPhase();
        }
    }

    /**
     * Marks a portable extinguisher used or not used. The status (USED / NOT_USED) is determined
     * automatically by collectChanges() in the group. No button reaches this path yet — see
     * {@code gui.map.input.ExtinguisherButtonGroup} — kept ready for when placement is introduced.
     */
    record ExtinguisherActionInput(Extinguisher extinguisher) implements InputAction {
        @Override
        public void apply(DeckMapController deckMapController, ClipsReportService clipsReportService,
            SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
        {
            Map<Extinguisher, ExtinguisherUsage> changes = deckMapController.collectExtinguisherChanges(extinguisher);
            Map<Extinguisher, ExtinguisherClipsStatus> remappedChanges = ClipsValuesMapper.remapToClips(changes, ClipsValuesMapper::toClips);
            clipsReportService.reportExtinguisherChanges(remappedChanges);
            deckMapController.repaint();
            solutionTree.refreshCurrentPhase();
        }
    }
}
