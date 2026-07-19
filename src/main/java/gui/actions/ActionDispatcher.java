package gui.actions;

import gui.solution.SolutionPhaseTree;
import gui.solution.SolutionResultsController;
import clips.ClipsReportService;
import gui.map.DeckMapController;

/**
 * Thin router for typed {@link InputAction}s: delegates each action to its own
 * {@link InputAction#apply} implementation. Replaced the legacy string {@code actionCommand}
 * parsing that used to live in {@code app.Main}'s {@code actionPerformed} method.
 */
public class ActionDispatcher {
    private final ClipsReportService clipsReportService;
    private final DeckMapController deckMapController;
    private final SolutionPhaseTree solutionTree;
    private final SolutionResultsController resultsController;

    public ActionDispatcher(ClipsReportService clipsReportService, DeckMapController deckMapController,
        SolutionPhaseTree solutionTree, SolutionResultsController resultsController)
    {
        this.clipsReportService = clipsReportService;
        this.deckMapController = deckMapController;
        this.solutionTree = solutionTree;
        this.resultsController = resultsController;
    }

    /**
     * Routes the given action to the appropriate service calls.
     */
    public void dispatch(InputAction action) {
        if (action == null)
            throw new IllegalArgumentException("InputAction cannot be null");
        action.apply(deckMapController, clipsReportService, solutionTree, resultsController);
    }
}
