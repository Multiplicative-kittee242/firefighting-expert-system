package gui.solution;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * Covers {@link SolutionPhaseTree#refreshCurrentPhase()}: every {@code InputAction} that reports an
 * incremental change to CLIPS relies on it to keep {@link SolutionResultsController}'s actions table
 * in sync. Before this method existed, such actions (door, ventilation, flammable-material prevention, …) never notified
 * phase-change listeners at all, so a completed action's row never disappeared from the table until
 * the user happened to switch phase-tree nodes — a long-standing bug this locks down.
 */
class SolutionPhaseTreeTest {

    @Test
    void refreshCurrentPhase_RenotifiesTheCurrentlySelectedPhaseWithoutChangingSelection() {
        SolutionPhaseTree phaseTree = new SolutionPhaseTree();
        List<SolutionTreeSection> notified = new ArrayList<>();
        phaseTree.addPhaseChangeListener(notified::add);

        phaseTree.getPhasesTree().setSelectionRow(2); // EVACUATION, per the tree's construction order
        assertThat(notified, contains(SolutionTreeSection.EVACUATION));
        int selectionBefore = phaseTree.getPhasesTree().getSelectionModel().getMinSelectionRow();

        notified.clear();
        phaseTree.refreshCurrentPhase();

        assertThat("refreshCurrentPhase must re-notify with the same (unchanged) phase",
            notified, contains(SolutionTreeSection.EVACUATION));
        assertThat("refreshCurrentPhase must not move the tree selection",
            phaseTree.getPhasesTree().getSelectionModel().getMinSelectionRow(), is(selectionBefore));
    }

    @Test
    void refreshCurrentPhase_WithNoSelectionNotifiesRoot() {
        SolutionPhaseTree phaseTree = new SolutionPhaseTree();
        List<SolutionTreeSection> notified = new ArrayList<>();
        phaseTree.addPhaseChangeListener(notified::add);

        phaseTree.refreshCurrentPhase();

        assertThat(notified, contains(SolutionTreeSection.ROOT));
    }
}
