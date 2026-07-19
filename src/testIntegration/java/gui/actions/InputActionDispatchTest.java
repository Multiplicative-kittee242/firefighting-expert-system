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
import config.loading.DeckMapTopologyConfig;
import domain.Extinguisher;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import domain.types.ExplosiveType;
import domain.types.ExtinguisherType;
import domain.types.PreventionType;
import gui.map.DeckMapController;
import gui.map.values.ExtinguisherUsage;
import gui.solution.SolutionPhaseTree;
import gui.solution.SolutionResultsController;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the 8 {@link InputAction} record implementations (direct .apply calls) and
 * {@link ActionDispatcher} delegation. Uses mocks for the four collaborators; no real Swing,
 * no real CLIPS engine. Verifies the per-action contract for report calls, repaint paths,
 * and the critical refresh/reset phase-tree behavior.
 * <p>
 * Deliberately isolates {@code apply()}'s own logic (call sequencing, {@code ClipsValuesMapper}
 * remapping, the {@code refreshCurrentPhase}/{@code resetPhaseAndNotify} contract) from whether
 * {@code DeckMapController}'s real {@code collect*Changes} produces correct values — that's
 * {@code gui.map.ClickThroughInputActionTest}'s job, with a real controller and real buttons. The
 * two are complementary, not duplicates: this class is the only place able to assert the negative
 * ({@code never()}) phase-tree call for every action type (only one gets that check in the
 * click-through suite) and the {@code repaintExplosionLocations} vs. plain {@code repaint()}
 * distinction for explosion prevention — both require a mocked, not real, {@code DeckMapController}.
 */
@ExtendWith(MockitoExtension.class)
class InputActionDispatchTest {

    private static final TopologyModel TOPOLOGY = DeckMapTopologyConfig.createDefault().buildTopologyModel();

    @Mock
    private DeckMapController deckMapController;
    @Mock
    private ClipsReportService clipsReportService;
    @Mock
    private SolutionPhaseTree solutionTree;
    @Mock
    private SolutionResultsController resultsController;

    @Test
    void apply_FireActionInput() {
        Location location = TOPOLOGY.location("a");
        FireIncidentSnapshot snapshot = mock(FireIncidentSnapshot.class);
        when(clipsReportService.reportFireIncident(location)).thenReturn(snapshot);

        InputAction action = new InputAction.FireActionInput(location);
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);

        verify(clipsReportService).reportFireIncident(location);
        verify(deckMapController).representFire(snapshot);
        verify(resultsController).updateEvents(location);
        verify(solutionTree).resetPhaseAndNotify();
        verify(solutionTree, never()).refreshCurrentPhase();
    }

    @Test
    void apply_VentilationActionInput() {
        Location location = TOPOLOGY.location("a");
        Map<Location, VentilationAction> changes = Map.of(location, VentilationAction.OFF);
        when(deckMapController.collectVentChanges(location)).thenReturn(changes);

        InputAction action = new InputAction.VentilationActionInput(location);
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);

        verify(clipsReportService).reportVentilationChanges(changes);
        verify(deckMapController).repaint();
        verify(solutionTree).refreshCurrentPhase();
        verify(solutionTree, never()).resetPhaseAndNotify();
    }

    @Test
    void apply_DoorSealingActionInput() {
        DeckMapController deckMapController = mock(DeckMapController.class);
        ClipsReportService clipsReportService = mock(ClipsReportService.class);
        SolutionPhaseTree solutionTree = mock(SolutionPhaseTree.class);
        SolutionResultsController resultsController = mock(SolutionResultsController.class);

        Link link = TOPOLOGY.link("ab");
        Map<Link, DoorState> changes = Map.of(link, DoorState.CLOSE);
        when(deckMapController.collectDoorChanges(link)).thenReturn(changes);

        InputAction action = new InputAction.DoorSealingActionInput(link);
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);

        verify(clipsReportService).reportDoorSealingChanges(changes);
        verify(deckMapController).repaint();
        verify(solutionTree).refreshCurrentPhase();
        verify(solutionTree, never()).resetPhaseAndNotify();
    }

    @Test
    void apply_FlammableActionInput() {
        Location location = TOPOLOGY.location("a");
        Map<Location, PreventionType> changes = Map.of(location, PreventionType.OIL);
        when(deckMapController.collectFlammableChanges(PreventionType.OIL, location)).thenReturn(changes);

        InputAction action = new InputAction.FlammableActionInput(location, PreventionType.OIL);
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);

        Map<Location, FlammablePreventionClipsAction> expectedRemapped =
            Map.of(location, FlammablePreventionClipsAction.PUMP_OUT);
        verify(clipsReportService).reportFlammablePreventionChanges(expectedRemapped);
        verify(deckMapController).repaint();
        verify(solutionTree).refreshCurrentPhase();
        verify(solutionTree, never()).resetPhaseAndNotify();
    }

    @Test
    void apply_MachineryDamageActionInput() {
        Location location = TOPOLOGY.location("a");
        Map<Location, MachineryDamageAction> changes = Map.of(location, MachineryDamageAction.STOP);
        when(deckMapController.collectMachineryDamageChanges(location)).thenReturn(changes);

        InputAction action = new InputAction.MachineryDamageActionInput(location);
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);

        verify(clipsReportService).reportMachineryDamagePreventionChanges(changes);
        verify(deckMapController).repaint();
        verify(solutionTree).refreshCurrentPhase();
        verify(solutionTree, never()).resetPhaseAndNotify();
    }

    @Test
    void apply_ExplosionPreventionActionInput() {
        Location location = TOPOLOGY.location("a");
        Map<Location, ExplosiveType> changes = Map.of(location, ExplosiveType.OIL);
        when(deckMapController.collectExplosionChanges(location, ExplosiveType.OIL)).thenReturn(changes);

        Set<Location> remaining = Set.of();
        Map<Location, ExplosionClipsAction> expectedRemapped =
            Map.of(location, ExplosionClipsAction.PUMP_OUT);
        when(clipsReportService.reportExplosionPreventionChanges(expectedRemapped)).thenReturn(remaining);

        InputAction action = new InputAction.ExplosionPreventionActionInput(location, ExplosiveType.OIL);
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);

        verify(clipsReportService).reportExplosionPreventionChanges(expectedRemapped);
        verify(deckMapController).repaintExplosionLocations(remaining);
        verify(deckMapController, never()).repaint();
        verify(solutionTree).refreshCurrentPhase();
        verify(solutionTree, never()).resetPhaseAndNotify();
    }

    @Test
    void apply_EvacuationActionInput() {
        Location location = TOPOLOGY.location("a");
        Map<Location, EvacuationStatus> changes = Map.of(location, EvacuationStatus.DONE);
        when(deckMapController.collectEvacChanges(location)).thenReturn(changes);

        InputAction action = new InputAction.EvacuationActionInput(location);
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);

        verify(clipsReportService).reportEvacuationChanges(changes);
        verify(deckMapController).repaint();
        verify(solutionTree).refreshCurrentPhase();
        verify(solutionTree, never()).resetPhaseAndNotify();
    }

    @Test
    void apply_ExtinguisherActionInput() {
        Location location = TOPOLOGY.location("a");
        Extinguisher extinguisher = new Extinguisher("test_ext", location, ExtinguisherType.CARBON_DIOXIDE);
        Map<Extinguisher, ExtinguisherUsage> changes = Map.of(extinguisher, ExtinguisherUsage.USED);
        when(deckMapController.collectExtinguisherChanges(extinguisher)).thenReturn(changes);

        InputAction action = new InputAction.ExtinguisherActionInput(extinguisher);
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);

        Map<Extinguisher, ExtinguisherClipsStatus> expectedRemapped =
            Map.of(extinguisher, ExtinguisherClipsStatus.USED);
        verify(clipsReportService).reportExtinguisherChanges(expectedRemapped);
        verify(deckMapController).repaint();
        verify(solutionTree).refreshCurrentPhase();
        verify(solutionTree, never()).resetPhaseAndNotify();
    }

    @Test
    void dispatch_NullActionThrowsIllegalArgumentException() {
        ActionDispatcher dispatcher = new ActionDispatcher(
            clipsReportService, deckMapController, solutionTree, resultsController);

        assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(null));
    }

    @Test
    void dispatch_DelegatesToActionApply() {
        Location location = TOPOLOGY.location("a");
        Map<Location, EvacuationStatus> changes = Map.of(location, EvacuationStatus.NONE);
        when(deckMapController.collectEvacChanges(location)).thenReturn(changes);

        ActionDispatcher dispatcher = new ActionDispatcher(
            clipsReportService, deckMapController, solutionTree, resultsController);

        InputAction action = new InputAction.EvacuationActionInput(location);
        dispatcher.dispatch(action);

        verify(clipsReportService).reportEvacuationChanges(changes);
    }
}
