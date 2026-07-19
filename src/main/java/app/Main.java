package app;

import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import config.loading.DeckMapTopologyConfig;
import gui.actions.ActionDispatcher;
import gui.actions.InputActionListener;
import clips.ClipsReadOnlyService;
import clips.ClipsReportService;
import clips.ExpertSystemService;
import gui.map.DeckMapController;
import domain.registry.TopologyModel;
import gui.Localization;
import gui.MainFrame;
import gui.solution.SolutionPhaseTree;
import gui.solution.SolutionResultsController;
import util.Charsets;
import util.ResourceUtil;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.PrintStream;
import java.util.Locale;

public class Main {
    private Main() {}

    private static void launch(DeckMapAssemblyConfig assemblyConfig, DeckMapConfig deckMapConfig, TopologyModel topology,
        ClipsReportService clipsReportService, ClipsReadOnlyService clipsReadOnlyService)
    {
        SolutionPhaseTree solutionTree = new SolutionPhaseTree();

        SolutionResultsController resultsController = new SolutionResultsController(clipsReadOnlyService, topology);
        solutionTree.addPhaseChangeListener(resultsController);

        InputActionListener actionListener = new InputActionListener();
        ImageIcon mapImage = new ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()));
        DeckMapController deckMapController = new DeckMapController(assemblyConfig, deckMapConfig, topology, mapImage, actionListener);
        solutionTree.addPhaseChangeListener(deckMapController);

        actionListener.setDispatcher(new ActionDispatcher(clipsReportService, deckMapController, solutionTree, resultsController));

        JFrame mainFrame = new MainFrame(
            deckMapController.getMapContainer(),
            solutionTree.getPhasesTree(),
            resultsController.getEventsTable(),
            resultsController.getActionsTable());
        mainFrame.setVisible(true);
    }

    public static void main(final String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, Charsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, Charsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Cannot set UTF-8 for log output: " + e.getMessage());
        }

        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run() {
                Localization.init(hasArgument() ? new Locale(args[0]) : null);

                DeckMapConfig deckMapConfig = DeckMapConfig.createDefault();
                DeckMapTopologyConfig topologyConfig = deckMapConfig.getTopologyConfig();

                TopologyModel topologyModel = topologyConfig.buildTopologyModel();
                DeckMapAssemblyConfig assemblyConfig = DeckMapAssemblyConfig.createDefault(
                    deckMapConfig.getGroupsConfig(), topologyModel.allHydrantOutlets());

                ExpertSystemService expertSystemService = new ExpertSystemService(topologyModel);

                launch(assemblyConfig, deckMapConfig, topologyModel, expertSystemService, expertSystemService);
            }

            private boolean hasArgument() {
                return args.length > 0 && !args[0].isEmpty();
            }
        });
    }
}
