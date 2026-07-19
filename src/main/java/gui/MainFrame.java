package gui;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Builds and configures the main application window (JFrame).
 * <p>
 * This class is responsible only for visual layout and component assembly.
 * It has no knowledge of business logic, services, or event dispatching.
 */
public class MainFrame extends JFrame {
    private static final String TITLE = Localization.get("title.main");

    public MainFrame(JPanel deckMapContent, JTree phasesTree, JTable eventsTable, JTable actionsTable) {
        super(TITLE);

        setSize(1350, 810);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocation(15, 87);
        setBackground(Color.white);

        // === Layout setup ===
        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);
        GridBagPlacer placer = new GridBagPlacer(this, gridBagLayout);

        // --- Upper area: map ---
        JPanel mapPanel = createMapPanel();
        placer.at(0, 0).span(3).fill(GridBagConstraints.HORIZONTAL).weight(1.0, 0.0).add(mapPanel);
        mapPanel.add(createMapContainerPanel(deckMapContent));

        // --- Bottom area: tree + tables ---
        JScrollPane treeScroll = new JScrollPane(phasesTree);
        placer.at(0, 1).span(1).fill(GridBagConstraints.BOTH).weight(0.3, 1.0).add(treeScroll);

        JScrollPane eventsScroll = new JScrollPane(eventsTable);
        eventsScroll.setMinimumSize(new Dimension(120, 150));
        eventsScroll.setMaximumSize(new Dimension(290, 200));
        placer.at(1, 1).span(1).fill(GridBagConstraints.BOTH).weight(0.35, 1.0).add(eventsScroll);

        JScrollPane actionsScroll = new JScrollPane(actionsTable);
        actionsScroll.setMinimumSize(new Dimension(280, 150));
        actionsScroll.setMaximumSize(new Dimension(520, 200));
        placer.at(2, 1).span(1).fill(GridBagConstraints.BOTH).weight(0.35, 1.0).add(actionsScroll);
    }

    private JPanel createMapPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.white);
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel.setMinimumSize(new Dimension(1226, 630));
        panel.setMaximumSize(new Dimension(1500, 650));
        return panel;
    }

    private static JPanel createMapContainerPanel(JPanel mapContainer) {
        JPanel panel = new JPanel();
        panel.add(mapContainer);
        panel.setOpaque(false);
        panel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        return panel;
    }

    private static final class GridBagPlacer {
        private final Container target;
        private final GridBagLayout layout;
        private final GridBagConstraints gbc = new GridBagConstraints();

        GridBagPlacer(Container target, GridBagLayout layout) {
            this.target = target;
            this.layout = layout;
        }

        GridBagPlacer at(int gridx, int gridy) {
            gbc.gridx = gridx;
            gbc.gridy = gridy;
            return this;
        }

        GridBagPlacer span(int gridwidth) {
            gbc.gridwidth = gridwidth;
            return this;
        }

        GridBagPlacer fill(int fill) {
            gbc.fill = fill;
            return this;
        }

        GridBagPlacer weight(double weightx, double weighty) {
            gbc.weightx = weightx;
            gbc.weighty = weighty;
            return this;
        }

        void add(JComponent component) {
            layout.setConstraints(component, gbc);
            target.add(component);
        }
    }
}
