package gui.solution;

import gui.Localization;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class SolutionPhaseTree {
    public static final String TREE_ROOT = Localization.get("tree.phases");
    public static final String TREE_PRIORITY_MEASURES = Localization.get("tree.priorities");
    public static final String TREE_EVACUATION_OF_PEOPLE = Localization.get("tree.evacuation");
    public static final String TREE_SEALING_OF_PREMISES = Localization.get("tree.sealing");
    public static final String TREE_LOCALIZATION_OF_FIRE_SOURCE = Localization.get("tree.localization");
    public static final String TREE_PREVENTION_OF_EXPLOSIONS_AND_FIRES = Localization.get("tree.prevention");
    public static final String TREE_FIREFIGHTING_PLAN = Localization.get("tree.firefighting");

    private final JTree tree;
    private final List<PhaseChangeListener> listeners = new LinkedList<>();

    public SolutionPhaseTree() {
        tree = createTree();
        expandAll(tree, true);
        tree.setEditable(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setMinimumSize(new Dimension(400, 150));
        tree.addTreeSelectionListener(event -> notifyPhaseChanged());
    }

    public JTree getPhasesTree() {
        return tree;
    }

    public void addPhaseChangeListener(PhaseChangeListener listener) {
        listeners.add(listener);
    }

    public void resetPhaseAndNotify() {
        int before = tree.getSelectionModel().getMinSelectionRow();
        tree.setSelectionRow(0);
        int after = tree.getSelectionModel().getMinSelectionRow();
        if (before == after)
            notifyPhaseChanged(); // still send notification in case the selected note has not actually changed
    }

    /**
     * Re-fires the current phase to all listeners without changing the tree selection.
     * Every action that reports an incremental change to CLIPS (door, ventilation,
     * flammable-material prevention, …) must call this afterward, or {@link SolutionResultsController}'s
     * actions table goes stale: it only re-queries CLIPS from {@link #notifyPhaseChanged()},
     * which otherwise only fires on an actual tree-selection change.
     */
    public void refreshCurrentPhase() {
        notifyPhaseChanged();
    }

    public void expandAll(JTree jTree, boolean expand) {
        TreeNode treeNode = (TreeNode) jTree.getModel().getRoot();
        expandAll(jTree, new TreePath(treeNode), expand);
    }

    private void expandAll(JTree jTree, TreePath treePath, boolean expand) {
        TreeNode treeNode = (TreeNode) treePath.getLastPathComponent();
        if (treeNode.getChildCount() >= 0) {
            Enumeration<? extends TreeNode> enumeration = treeNode.children();
            while (enumeration.hasMoreElements()) {
                TreeNode treeNode2 = enumeration.nextElement();
                TreePath treePath2 = treePath.pathByAddingChild(treeNode2);
                expandAll(jTree, treePath2, expand);
            }
        }
        if (expand) {
            jTree.expandPath(treePath);
        } else {
            jTree.collapsePath(treePath);
        }
    }

    private void notifyPhaseChanged() {
        for (PhaseChangeListener listener : listeners)
            listener.onPhaseChanged(getSelectedPhase());
    }

    private SolutionTreeSection getSelectedPhase() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node != null && node.getUserObject() instanceof ResultsTreeNode data) {
            return data.section();
        } else {
            return SolutionTreeSection.ROOT;
        }
    }

    private static JTree createTree() {
        DefaultMutableTreeNode treeRoot = createTreeNode(SolutionTreeSection.ROOT, TREE_ROOT);
        DefaultMutableTreeNode subtree1 = createTreeNode(SolutionTreeSection.PRIORITY_MEASURES, TREE_PRIORITY_MEASURES);
        DefaultMutableTreeNode subtree2 = createTreeNode(SolutionTreeSection.EVACUATION, TREE_EVACUATION_OF_PEOPLE);
        DefaultMutableTreeNode subtree3 = createTreeNode(SolutionTreeSection.SEALING, TREE_SEALING_OF_PREMISES);
        DefaultMutableTreeNode subtree4 = createTreeNode(SolutionTreeSection.PREVENTION, TREE_PREVENTION_OF_EXPLOSIONS_AND_FIRES);
        DefaultMutableTreeNode subtree5 = createTreeNode(SolutionTreeSection.LOCALIZATION, TREE_LOCALIZATION_OF_FIRE_SOURCE);
        DefaultMutableTreeNode subtree6 = createTreeNode(SolutionTreeSection.FIREFIGHTING, TREE_FIREFIGHTING_PLAN);

        treeRoot.add(subtree1);
        subtree1.add(subtree2);
        subtree1.add(subtree3);
        treeRoot.add(subtree5);
        subtree5.add(subtree4);
        treeRoot.add(subtree6);

        return new JTree(treeRoot);
    }

    private static DefaultMutableTreeNode createTreeNode(SolutionTreeSection section, String displayName) {
        return new DefaultMutableTreeNode(new ResultsTreeNode(section, displayName));
    }

    public interface PhaseChangeListener {
        void onPhaseChanged(SolutionTreeSection newPhase);
    }

    public record ResultsTreeNode(SolutionTreeSection section, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }
}
