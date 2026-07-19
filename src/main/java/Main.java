import CLIPSJNI.Environment;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

public class Main implements ActionListener {
    private static final String CLIPS_RULES_BASE = "clips/feis.clp";

    private static final String ELEMENT_SENSOR = "sn";
    private static final String ELEMENT_BUTTON = "fb";
    private static final String ELEMENT_EVACUATION = "ev";
    private static final String ELEMENT_VENTILATION = "vn";
    private static final String ELEMENT_DOOR = "di";
    private static final String ELEMENT_EXPLOSIVE = "ex";
    private static final String ELEMENT_ISOLATION = "is";
    private static final String ELEMENT_ISOLATION_MECH = "mch";

    private static final String EXPLOSIVE_TYPE_OIL = "oil";
    private static final String EXPLOSIVE_TYPE_AIR = "air";
    private static final String EXPLOSIVE_TYPE_OTHER = "rgt";

    private final String TITLE = Localization.get("title.main");

    private final String PHASE_EVACUATION = Localization.get("phase.evacuation");
    private final String PHASE_GERMETISATION = Localization.get("phase.germetisation");
    private final String PHASE_PREVENTION_OF_FIRE = Localization.get("phase.prevention.fire");
    private final String PHASE_MECHANICAL_DAMAGE = Localization.get("phase.prevention.mechanical");
    private final String PHASE_PREVENTION_OF_EXPLOSION = Localization.get("phase.prevention.explosion");
    private final String PHASE_LOCALIZATION = Localization.get("phase.localization");
    private final String PHASE_FIRE_EXTINGUISHING = Localization.get("phase.extinguishing");

    private final String TREE_PHASES_OF_SOLUTION = Localization.get("tree.phases");
    private final String TREE_PRIORITY_MEASURES = Localization.get("tree.priorities");
    private final String TREE_EVACUATION_OF_PEOPLE = Localization.get("tree.evacuation");
    private final String TREE_SEALING_OF_PREMISES = Localization.get("tree.sealing");
    private final String TREE_LOCALIZATION_OF_FIRE_SOURCE = Localization.get("tree.localization");
    private final String TREE_PREVENTION_OF_EXPLOSIONS_AND_FIRES = Localization.get("tree.prevention");
    private final String TREE_FIRE_EXTINGUISHING_PLAN = Localization.get("tree.extinguishing");

    //================================================================
    // Root: JFrame (jfrm) with GridBagLayout
    //   1. JScrollPane jtreescrl -> JTree (left phase tree)
    //   2. JScrollPane jtblEventsTableScrl -> FireEventsTable + EventsTableModel
    //   3. JScrollPane jtblActionsTableScrl -> RecommendedActionsTable + ActionsTableModel + mouse listener for popups
    //   4. JPanel jppnl (main map container) -> nested overlays + jlab (map image + absolute positioned buttons)
    //      -> jpnlCont -> PaintEvac -> PaintExpl -> PaintFireLines -> jlab (JLabel)
    //      Buttons (custom *Button inners) added to jlab with setBounds (absolute)
    // Listeners: Main as ActionListener for most buttons; anonymous for tree and table2 mouse.
    //================================================================
    Environment clips;
    JPanel jppnl;
    JPanel jpnlCont;
    JLabel jlab;
    String pressed;
    String fire = null;
    String threat = null;
    String evacuate = null;
    boolean sensVisible = true;
    boolean fireVisible = true;
    boolean evacVisible = false;
    boolean ventVisible = false;
    boolean drVisible = false;
    boolean isolVisible = false;
    boolean isol_mechVisible = false;
    boolean explVisible = false;
    boolean hydr_countVisible = false;
    boolean ext_countVisible = false;
    boolean hydrVisible = false;
    boolean hydr_outVisible = false;
    boolean hydr_extVisible = false;
    boolean hydr_ext_bVisible = false;
    boolean hydr_ext_b_fromVisible = false;
    int[] fill_fire = new int[20];
    int[] fill_threat = new int[20];
    int[] fill_evac = new int[20];
    int[] fill_expl = new int[4];
    int[] fill_lines = new int[20];
    int index = -1;
    int indexthreat = -1;
    int indexevac = -1;
    int indexexpl = -1;
    int indexlines = -1;
    int hydr_out_index = 0;
    int hydr_ext_index = 0;
    int hydr_ext_b_index = 0;
    int hydr_ext_b_from_index = 0;
    // colors
    static Color red = new Color(255, 40, 2);
    static Color dark_red = new Color(161, 24, 0);
    static Color mid_dark_red = new Color(238, 36, 0);
    static Color pastele_red = new Color(255, 123, 123);
    static Color green = new Color(154, 205, 50);
    static Color dark_green = new Color(115, 153, 39);
    static Color blue = new Color(100, 149, 237);
    static Color sky_blue = new Color(135, 206, 235);
    static Color orange = new Color(255, 140, 0);
    static Color dark_orange = new Color(196, 108, 0);
    static Color pastele_orange = new Color(255, 222, 173);
    static Color brown = new Color(139, 16, 19);
    static Color co_gray = new Color(128, 128, 128);
    static Color pastele_grey = new Color(220, 220, 220);
    static Color light_pastele_grey = new Color(251, 251, 251);
    // coordinates
    static int[][][] locations = new int[][][]{new int[][]{{247, 247, 207, 207, 503, 503}, {438, 521, 521, 614, 614, 438}, {6}}, new int[][]{{505, 505, 843, 843}, {480, 614, 614, 480}, {4}}, new int[][]{{845, 845, 1019, 1019}, {480, 614, 614, 480}, {4}}, new int[][]{{207, 207, 245, 245, 1019, 1019}, {264, 414, 414, 301, 301, 264}, {6}}, new int[][]{{247, 247, 503, 503}, {303, 437, 437, 303}, {4}}, new int[][]{{505, 505, 747, 747}, {303, 478, 478, 303}, {4}}, new int[][]{{749, 749, 803, 803}, {345, 397, 397, 345}, {4}}, new int[][]{{805, 805, 843, 843}, {303, 397, 397, 303}, {4}}, new int[][]{{749, 749, 803, 803}, {303, 343, 343, 303}, {4}}, new int[][]{{207, 207, 1019, 1019}, {15, 262, 262, 15}, {4}}, new int[][]{{749, 749, 843, 843}, {399, 438, 438, 399}, {4}}, new int[][]{{749, 749, 843, 843}, {440, 478, 478, 440}, {4}}, new int[][]{{845, 845, 939, 939}, {345, 478, 478, 345}, {4}}, new int[][]{{845, 845, 939, 939}, {303, 343, 343, 303}, {4}}, new int[][]{{941, 941, 1019, 1019}, {303, 390, 390, 303}, {4}}, new int[][]{{941, 941, 1019, 1019}, {392, 478, 478, 392}, {4}}, new int[][]{{207, 207, 245, 245}, {416, 519, 519, 416}, {4}}};
    static int[][] sensors = new int[][]{{274, 480}, {457, 528}, {559, 555}, {770, 507}, {879, 568}, {216, 351}, {260, 273}, {321, 273}, {474, 273}, {787, 273}, {814, 273}, {274, 331}, {539, 426}, {709, 337}, {525, 72}, {525, 168}, {676, 168}, {753, 72}, {828, 168}, {980, 72}, {980, 120}, {980, 168}, {751, 442}, {864, 354}};
    static int[][] fire_button = new int[][]{{750, 385}, {831, 369}, {750, 331}, {831, 426}, {927, 331}, {1007, 378}, {1007, 466}, {233, 507}};
    static int[][] evacuations = new int[][]{{461, 440}, {801, 482}, {977, 482}, {461, 305}, {705, 305}, {782, 347}, {822, 305}, {782, 305}, {772, 442}, {898, 347}, {977, 305}, {224, 433}};
    static int[][] ventilations = new int[][]{{482, 440}, {822, 482}, {998, 482}, {482, 305}, {726, 305}, {998, 17}, {918, 347}, {998, 305}};
    static int[][] doors = new int[][]{{245, 452, 18, 30}, {503, 355, 18, 30}, {827, 444, 18, 30}, {787, 354, 18, 30}, {400, 301, 30, 18}, {919, 246, 30, 18}, {183, 378, 22, 34}, {211, 414, 30, 18}, {645, 301, 30, 18}, {944, 478, 30, 18}, {939, 351, 18, 30}, {809, 381, 30, 18}, {787, 309, 18, 30}, {843, 309, 18, 30}, {843, 354, 18, 30}, {482, 530, 22, 34}, {998, 265, 22, 35}, {998, 394, 22, 34}, {807, 301, 34, 23}};
    static int[][] explosions = new int[][]{{998, 38}, {482, 326}, {482, 461}};
    static int[][] explosion_dots = new int[][]{{596, 195}, {295, 393}, {268, 555}};
    static int[][] isolations = new int[][]{{977, 38}, {461, 326}, {918, 368}, {793, 442}};
    static int[][] isolations_mech = new int[][]{{956, 38}, {440, 326}};
    static int[][][] borders = new int[][][]{new int[][]{{504, 504}, {479, 615}, {2}}, new int[][]{{246, 504}, {437, 437}, {2}}, new int[][]{{504, 504}, {437, 479}, {2}}, new int[][]{{206, 246, 246}, {520, 520, 437}, {3}}, new int[][]{{844, 844}, {479, 615}, {2}}, new int[][]{{504, 748}, {479, 479}, {2}}, new int[][]{{748, 844}, {479, 479}, {2}}, new int[][]{{844, 940}, {479, 479}, {2}}, new int[][]{{940, 1020}, {479, 479}, {2}}, new int[][]{{246, 246, 504}, {437, 302, 302}, {3}}, new int[][]{{504, 748}, {302, 302}, {2}}, new int[][]{{804, 844}, {302, 302}, {2}}, new int[][]{{206, 1020}, {263, 263}, {2}}, new int[][]{{748, 804}, {302, 302}, {2}}, new int[][]{{844, 940}, {302, 302}, {2}}, new int[][]{{940, 1020}, {302, 302}, {2}}, new int[][]{{206, 246}, {415, 415}, {2}}, new int[][]{{504, 504}, {302, 437}, {2}}, new int[][]{{246, 246}, {415, 437}, {2}}, new int[][]{{748, 748}, {344, 398}, {2}}, new int[][]{{748, 748}, {302, 344}, {2}}, new int[][]{{748, 748}, {398, 437}, {2}}, new int[][]{{748, 748}, {437, 479}, {2}}, new int[][]{{804, 804}, {344, 398}, {2}}, new int[][]{{748, 804}, {344, 344}, {2}}, new int[][]{{748, 844}, {398, 398}, {2}}, new int[][]{{804, 804}, {302, 344}, {2}}, new int[][]{{804, 844}, {398, 398}, {2}}, new int[][]{{844, 844}, {344, 398}, {2}}, new int[][]{{844, 844}, {302, 344}, {2}}, new int[][]{{748, 844}, {437, 437}, {2}}, new int[][]{{844, 844}, {398, 437}, {2}}, new int[][]{{844, 844}, {437, 479}, {2}}, new int[][]{{844, 940}, {344, 344}, {2}}, new int[][]{{940, 940}, {344, 391}, {2}}, new int[][]{{940, 940}, {391, 479}, {2}}, new int[][]{{940, 940}, {302, 344}, {2}}, new int[][]{{940, 1020}, {391, 391}, {2}}};
    static int[][] hydrants_count = new int[][]{{434, 440}, {774, 482}, {950, 482}, {515, 266}, {434, 305}, {678, 401}, {751, 347}, {807, 305}, {751, 305}, {950, 17}, {751, 401}, {751, 442}, {870, 347}, {870, 305}, {950, 305}, {943, 394}, {209, 418}};
    static int[][] hydrants_ext = new int[][]{{467, 440}, {807, 482}, {983, 482}, {515, 266}, {467, 305}, {711, 305}, {751, 347}, {807, 305}, {751, 305}, {983, 17}, {751, 401}, {772, 442}, {903, 347}, {903, 305}, {983, 305}, {943, 394}, {209, 418}};
    static int[][] hydrants = new int[][]{{360, 266}, {880, 266}, {728, 330}, {972, 210}, {906, 400}, {986, 445}, {226, 486}};
    static int[][][] hydrant_out_locations = new int[][][]{new int[][]{{434, 457}, {0}, {1}}, new int[][]{{774, 499}, {0}, {1}}, new int[][]{{950, 499}, {0}, {1}}, new int[][]{{515, 283}, {0}, {1}}, new int[][]{{434, 322}, {0}, {1}}, new int[][]{{678, 418}, {0}, {1}}, new int[][]{{751, 364}, {0}, {2}}, new int[][]{{807, 322}, {0}, {2}}, new int[][]{{751, 322}, {0}, {2}}, new int[][]{{950, 34}, {0}, {1}}, new int[][]{{751, 418}, {0}, {1}}, new int[][]{{751, 459}, {0}, {1}}, new int[][]{{870, 364}, {0}, {1}}, new int[][]{{870, 322}, {0}, {1}}, new int[][]{{950, 322}, {0}, {1}}, new int[][]{{943, 411}, {0}, {1}}, new int[][]{{209, 435}, {0}, {2}}};
    static int[][][] hydrant_ext_locations = new int[][][]{new int[][]{{434, 457}, {0}, {1}}, new int[][]{{774, 499}, {0}, {1}}, new int[][]{{950, 499}, {0}, {1}}, new int[][]{{515, 283}, {0}, {1}}, new int[][]{{434, 322}, {0}, {1}}, new int[][]{{678, 418}, {0}, {1}}, new int[][]{{751, 364}, {0}, {2}}, new int[][]{{807, 322}, {0}, {2}}, new int[][]{{751, 322}, {0}, {2}}, new int[][]{{950, 34}, {0}, {1}}, new int[][]{{751, 418}, {0}, {1}}, new int[][]{{751, 459}, {0}, {1}}, new int[][]{{870, 364}, {0}, {1}}, new int[][]{{870, 322}, {0}, {1}}, new int[][]{{950, 322}, {0}, {1}}, new int[][]{{943, 411}, {0}, {1}}, new int[][]{{209, 435}, {0}, {2}}};
    // labels
    static String[] locations_labels = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q"};
    static String[] sensor_labels = new String[]{"A1", "A2", "B1", "B2", "C", "D1", "D2", "D3", "D4", "D5", "D6", "E", "F1", "F2", "J1", "J2", "J3", "J4", "J5", "J6", "J7", "J8", "L", "M"};
    static String[] fire_labels = new String[]{"G", "H", "I", "K", "N", "O", "P", "Q"};
    static String[] evac_labels = new String[]{"A", "B", "C", "E", "F", "G", "H", "I", "L", "M", "O", "Q"};
    static String[] ventil_labels = new String[]{"A", "B", "C", "E", "F", "J", "M", "O"};
    static String[] door_labels = new String[]{"AQ", "EF", "LM", "GH", "DE", "DJ", "DR", "DQ", "DF", "CP", "MO", "HK", "HI", "HN", "HM", "AB", "DT", "PT", "DH"};
    static String[] expl_labels = new String[]{"J", "E", "A"};
    static String[] expl_object_labels = new String[]{EXPLOSIVE_TYPE_AIR, EXPLOSIVE_TYPE_OIL, EXPLOSIVE_TYPE_OTHER};
    static String[] isol_labels = new String[]{"J", "E", "M", "L"};
    static String[] isol_mech_labels = new String[]{"J", "E"};
    static String[] isol_object_labels = new String[]{EXPLOSIVE_TYPE_OIL, EXPLOSIVE_TYPE_OIL, "cls", "cls"};
    static String[] border_labels = new String[]{"AB", "AE", "AF", "AQ", "BC", "BF", "BL", "CM", "CP", "DE", "DF", "DH", "DJ", "DI", "DN", "DO", "DQ", "EF", "EQ", "FG", "FI", "FK", "FL", "GH", "GI", "GK", "HI", "HK", "HM", "HN", "KL", "KM", "LM", "MN", "MO", "MP", "NO", "OP"};
    static String[] hydr_count_labels = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q"};
    static String[] hydr_labels = new String[]{"hydr_d1", "hydr_d2", "hydr_f", "hydr_j", "hydr_m", "hydr_p", "hydr_q"};
    static String[] hydr_out_labels = new String[]{"hydr_d1", "hydr_d2", "hydr_f", "hydr_j", "hydr_m", "hydr_p", "hydr_q"};
    static String[] hydr_loc_labels = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q"};

    JFrame jfrm = new JFrame(TITLE);

    DefaultMutableTreeNode tree = new DefaultMutableTreeNode(TREE_PHASES_OF_SOLUTION);
    DefaultMutableTreeNode subtree1 = new DefaultMutableTreeNode(TREE_PRIORITY_MEASURES);
    DefaultMutableTreeNode subtree2 = new DefaultMutableTreeNode(TREE_EVACUATION_OF_PEOPLE);
    DefaultMutableTreeNode subtree3 = new DefaultMutableTreeNode(TREE_SEALING_OF_PREMISES);
    DefaultMutableTreeNode subtree5 = new DefaultMutableTreeNode(TREE_LOCALIZATION_OF_FIRE_SOURCE);
    DefaultMutableTreeNode subtree4 = new DefaultMutableTreeNode(TREE_PREVENTION_OF_EXPLOSIONS_AND_FIRES);
    DefaultMutableTreeNode subtree6 = new DefaultMutableTreeNode(TREE_FIRE_EXTINGUISHING_PLAN);

    JTree jtree = new JTree(tree);
    JScrollPane jtreescrl = new JScrollPane(jtree);
    EventsTableModel eventsTableModel = new EventsTableModel();
    ActionsTableModel actionsTableModel = new ActionsTableModel();
    JTable jtblEventsTable = new JTable(eventsTableModel);
    JTable jtblActionsTable = new JTable(actionsTableModel);
    JScrollPane jtblEventsTableScrl = new JScrollPane(jtblEventsTable);
    JScrollPane jtblActionsTableScrl = new JScrollPane(jtblActionsTable);
    JPopupMenu jpu = new JPopupMenu();
    ImageIcon map = new ImageIcon(getResource(Localization.getMapImageFile()));
    PaintEvac pntEvac = new PaintEvac();
    PaintExpl pntExpl = new PaintExpl();
    PaintFireLines pntLines = new PaintFireLines();
    FireSensorButton[] sens = new FireSensorButton[]{new FireSensorButton("K"), new FireSensorButton("K"), new FireSensorButton("K"), new FireSensorButton("K"), new FireSensorButton("K"), new FireSensorButton("K"), new FireSensorButton("K"), new FireSensorButton("RI"), new FireSensorButton("K"), new FireSensorButton("K"), new FireSensorButton("RI"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("RI"), new FireSensorButton("T"), new FireSensorButton("T"), new FireSensorButton("K")};
    FireButton[] fire_btn = new FireButton[]{new FireButton(), new FireButton(), new FireButton(), new FireButton(), new FireButton(), new FireButton(), new FireButton(), new FireButton()};
    EvacuateButton[] evac = new EvacuateButton[]{new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton(), new EvacuateButton()};
    AirIsolButton[] ventil = new AirIsolButton[]{new AirIsolButton(Localization.get("label.letter.b")), new AirIsolButton(Localization.get("label.letter.b")), new AirIsolButton(Localization.get("label.letter.b")), new AirIsolButton("C"), new AirIsolButton("T"), new AirIsolButton("C"), new AirIsolButton("T"), new AirIsolButton("T")};
    DoorIsolButton[] dr = new DoorIsolButton[]{new DoorIsolButton("custom", "vertical", "right", "bottom", "no"), new DoorIsolButton("custom", "vertical", "right", "top", "no"), new DoorIsolButton("custom", "vertical", "left", "top", "no"), new DoorIsolButton("custom", "vertical", "left", "bottom", "no"), new DoorIsolButton("custom", "horisontal", "left", "bottom", "no"), new DoorIsolButton("custom", "horisontal", "left", "top", "no"), new DoorIsolButton("fire", "vertical", "", "top", "no"), new DoorIsolButton("custom", "horisontal", "left", "bottom", "no"), new DoorIsolButton("custom", "horisontal", "left", "bottom", "no"), new DoorIsolButton("custom", "horisontal", "left", "bottom", "no"), new DoorIsolButton("custom", "vertical", "right", "top", "no"), new DoorIsolButton("custom", "horisontal", "left", "top", "no"), new DoorIsolButton("custom", "vertical", "left", "bottom", "no"), new DoorIsolButton("custom", "vertical", "right", "bottom", "no"), new DoorIsolButton("custom", "vertical", "right", "bottom", "no"), new DoorIsolButton("fire", "vertical", "", "bottom", "no"), new DoorIsolButton("fire", "vertical", "", "bottom", "no"), new DoorIsolButton("fire", "vertical", "", "bottom", "no"), new DoorIsolButton("fire", "horisontal", "", "", "no")};
    ExplosiveButton[] expl = new ExplosiveButton[]{new ExplosiveButton("A"), new ExplosiveButton("O"), new ExplosiveButton("R")};
    IsolationButton[] isol = new IsolationButton[]{new IsolationButton("O"), new IsolationButton("O"), new IsolationButton("C"), new IsolationButton("C")};
    IsolationButton[] isol_mech = new IsolationButton[]{new IsolationButton("M"), new IsolationButton("M")};
    HydrCountButton[] hydr_count = new HydrCountButton[]{new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "short"), new HydrCountButton("0", "0", "short"), new HydrCountButton("0", "0", "short"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "full"), new HydrCountButton("0", "0", "short")};
    ExtCountButton[] ext_count = new ExtCountButton[]{new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short"), new ExtCountButton("0", "0", "short")};
    HydrButton[] hydr = new HydrButton[]{new HydrButton(2, 2), new HydrButton(2, 2), new HydrButton(1, 1), new HydrButton(3, 3), new HydrButton(2, 2), new HydrButton(2, 2), new HydrButton(1, 1)};
    HydrOutButton[] hydr_out = new HydrOutButton[20];
    HydrExtButton[] hydr_ext = new HydrExtButton[20];
    HydrExtBorderToButton[] hydr_ext_b = new HydrExtBorderToButton[20];
    HydrExtBorderFromButton[] hydr_ext_b_from = new HydrExtBorderFromButton[20];

    private URL getResource(String path) {
        return getClass().getResource("/" + path);
    }

    public void updateActionsTable() {
        String doorPair;
        String ventOffRooms;
        String germDoorClose;
        String germDoorKeepOpen;
        String isolationRooms;
        String evacuationRooms;
        String isolationMechRooms;
        String explosionRooms;
        String fireLineRooms;
        String firePlanRooms;
        DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)jtree.getLastSelectedPathComponent();
        String treeNodeName = defaultMutableTreeNode.getUserObject().toString();
        if (treeNodeName.equals(TREE_PHASES_OF_SOLUTION)) {
            if (actionsTableRowCount() > 0) {
                for (int i = 0; i < actionsTableRowCount(); ++i)
                    actionsTableModel.removeRow();
            }
            showAll();
        }
        if (treeNodeName.equals(TREE_PRIORITY_MEASURES)) {
            String evacData = clips.eval("(collect-evac-evacuation to-evacuate)").toString();
            evacuationRooms = evacData.substring(1, evacData.length() - 1);
            ventOffRooms = (germDoorClose = clips.eval("(collect-germ-loc to-off)").toString()).substring(1, germDoorClose.length() - 1);
            germDoorClose = (germDoorKeepOpen = clips.eval("(collect-germ-door to-close)").toString()).substring(1, germDoorKeepOpen.length() - 1);
            if (actionsTableRowCount() > 0) {
                for (int n = 0; n < actionsTableRowCount(); ++n)
                    actionsTableModel.removeRow();
            }
            for (int n = 1; n <= evacuationRooms.length(); ++n)
                actionsTableModel.addRow(PHASE_EVACUATION, actionsTableRowCount() + 1, evacuationRooms.substring(n - 1, n).toUpperCase(), String.format(Localization.get("message.evacuation.room"), evacuationRooms.substring(n - 1, n).toUpperCase()));
            for (int n = 1; n <= ventOffRooms.length(); ++n)
                actionsTableModel.addRow(PHASE_GERMETISATION, actionsTableRowCount() + 1, ventOffRooms.substring(n - 1, n).toUpperCase(), String.format(Localization.get("message.germetisation.ventilation"), ventOffRooms.substring(n - 1, n).toUpperCase()));
            for (int n = 0; n < germDoorClose.length() / 2 - 1; ++n) {
                doorPair = germDoorClose.substring(n * 2, n * 2 + 2);
                if (doorPair.charAt(0) >= doorPair.charAt(1))
                    continue;
                actionsTableModel.addRow(PHASE_GERMETISATION, actionsTableRowCount() + 1, doorPair.substring(0, 1).toUpperCase() + ", " + doorPair.substring(1, 2).toUpperCase(), String.format(Localization.get("message.germetisation.door.close"), doorPair.substring(0, 1).toUpperCase(), doorPair.substring(1, 2).toUpperCase()));
            }
            showFirstTime();
        }
        if (treeNodeName.equals(TREE_EVACUATION_OF_PEOPLE)) {
            String evacData = clips.eval("(collect-evac-evacuation to-evacuate)").toString();
            evacuationRooms = evacData.substring(1, evacData.length() - 1);
            if (actionsTableRowCount() > 0) {
                for (int i = 0; i < actionsTableRowCount(); ++i)
                    actionsTableModel.removeRow();
            }
            for (int i = 1; i <= evacuationRooms.length(); ++i)
                actionsTableModel.addRow(PHASE_EVACUATION, actionsTableRowCount() + 1, evacuationRooms.substring(i - 1, i).toUpperCase(), String.format(Localization.get("message.evacuation.room"), evacuationRooms.substring(i - 1, i).toUpperCase()));
            showEvacuation();
        }
        if (treeNodeName.equals(TREE_SEALING_OF_PREMISES)) {
            String ventOffData = clips.eval("(collect-germ-loc to-off)").toString();
            ventOffRooms = ventOffData.substring(1, ventOffData.length() - 1);
            germDoorClose = (germDoorKeepOpen = clips.eval("(collect-germ-door to-close)").toString()).substring(1, germDoorKeepOpen.length() - 1);
            String keepOpenData = clips.eval("(collect-germ-door keep-open)").toString();
            germDoorKeepOpen = keepOpenData.substring(1, keepOpenData.length() - 1);
            if (actionsTableRowCount() > 0) {
                for (int n = 0; n < actionsTableRowCount(); ++n)
                    actionsTableModel.removeRow();
            }
            for (int n = 1; n <= ventOffRooms.length(); ++n)
                actionsTableModel.addRow(PHASE_GERMETISATION, actionsTableRowCount() + 1, ventOffRooms.substring(n - 1, n).toUpperCase(), String.format(Localization.get("message.germetisation.ventilation"), ventOffRooms.substring(n - 1, n).toUpperCase()));
            for (int n = 0; n <= germDoorClose.length() / 2 - 1; ++n) {
                doorPair = germDoorClose.substring(n * 2, n * 2 + 2);
                if (doorPair.charAt(0) >= doorPair.charAt(1))
                    continue;
                actionsTableModel.addRow(PHASE_GERMETISATION, actionsTableRowCount() + 1, doorPair.substring(0, 1).toUpperCase() + ", " + doorPair.substring(1, 2).toUpperCase(), String.format(Localization.get("message.germetisation.door.close"), doorPair.substring(0, 1).toUpperCase(), doorPair.substring(1, 2).toUpperCase()));
            }
            for (int n = 0; n <= germDoorKeepOpen.length() / 2 - 1; ++n) {
                doorPair = germDoorKeepOpen.substring(n * 2, n * 2 + 2);
                if (doorPair.charAt(0) >= doorPair.charAt(1))
                    continue;
                actionsTableModel.addRow(PHASE_GERMETISATION, actionsTableRowCount() + 1, doorPair.substring(0, 1).toUpperCase() + ", " + doorPair.substring(1, 2).toUpperCase(), String.format(Localization.get("message.germetisation.door.close.with.hose"), doorPair.substring(0, 1).toUpperCase(), doorPair.substring(1, 2).toUpperCase()));
            }
            showGermetisation();
        }
        if (treeNodeName.equals(TREE_PREVENTION_OF_EXPLOSIONS_AND_FIRES)) {
            String isolationData = clips.eval("(collect-action-phase isolation)").toString();
            isolationRooms = isolationData.substring(1, isolationData.length() - 1);
            String mechData = clips.eval("(collect-isol-mech stop)").toString();
            isolationMechRooms = mechData.substring(1, mechData.length() - 1);
            String explosionData = clips.eval("(collect-action-phase explosion)").toString();
            explosionRooms = explosionData.substring(1, explosionData.length() - 1);
            if (actionsTableRowCount() > 0) {
                for (int n = 0; n < actionsTableRowCount(); ++n)
                    actionsTableModel.removeRow();
            }
            if (!isolationRooms.isEmpty()) {
                for (int n = 1; n <= isolationRooms.length(); ++n)
                    actionsTableModel.addRow(PHASE_PREVENTION_OF_FIRE, actionsTableRowCount() + 1, isolationRooms.substring(n - 1, n).toUpperCase(), String.format(Localization.get("message.prevention.fire"), isolationRooms.substring(n - 1, n).toUpperCase()));
            }
            if (!isolationMechRooms.isEmpty()) {
                for (int n = 1; n <= isolationMechRooms.length(); ++n)
                    actionsTableModel.addRow(PHASE_MECHANICAL_DAMAGE, actionsTableRowCount() + 1, isolationMechRooms.substring(n - 1, n).toUpperCase(), String.format(Localization.get("message.prevention.mechanical"), isolationMechRooms.substring(n - 1, n).toUpperCase()));
            }
            if (!explosionRooms.isEmpty()) {
                for (int n = 1; n <= explosionRooms.length(); ++n)
                    actionsTableModel.addRow(PHASE_PREVENTION_OF_EXPLOSION, actionsTableRowCount() + 1, explosionRooms.substring(n - 1, n).toUpperCase(), String.format(Localization.get("message.prevention.explosion"), explosionRooms.substring(n - 1, n).toUpperCase()));
            }
            showPrevention();
        }
        if (treeNodeName.equals(TREE_LOCALIZATION_OF_FIRE_SOURCE)) {
            String fireLineData = clips.eval("(get-fire-line-locations)").toString();
            fireLineRooms = fireLineData.substring(1, fireLineData.length() - 1);
            String isolationData = clips.eval("(collect-action-phase isolation)").toString();
            isolationRooms = isolationData.substring(1, isolationData.length() - 1);
            String mechData = clips.eval("(collect-isol-mech stop)").toString();
            isolationMechRooms = mechData.substring(1, mechData.length() - 1);
            String explosionData = clips.eval("(collect-action-phase explosion)").toString();
            explosionRooms = explosionData.substring(1, explosionData.length() - 1);
            if (!isolationData.isEmpty() || !mechData.isEmpty() || !explosionData.isEmpty()) {
                int roomIndex;
                if (actionsTableRowCount() > 0) {
                    for (roomIndex = 0; roomIndex < actionsTableRowCount(); ++roomIndex)
                        actionsTableModel.removeRow();
                }
                goto_get_hydr_for_location: for (roomIndex = 1; roomIndex <= fireLineRooms.length(); ++roomIndex) {
                    String locationCode = fireLineRooms.substring(roomIndex - 1, roomIndex);
                    String hydrData = clips.eval("(get-hydr-for-location " + locationCode + ")").toString();
                    String hydrName = "";
                    String separator = " ";
                    int lastSpaceIndex = -1;
                    if (hydrData.contains(separator) && !hydrData.equals("()")) {
                        hydrData = hydrData.substring(1, hydrData.length() - 1);
                        for (int i = 0; i < hydrData.length(); ++i) {
                            if (hydrData.charAt(i) != ' ')
                                continue;
                            hydrName = hydrData.substring(lastSpaceIndex + 1, i);
                            actionsTableModel.addRow(PHASE_LOCALIZATION, actionsTableRowCount() + 1, locationCode.toUpperCase(), String.format(Localization.get("message.localization.hydrant"), hydrName));
                            lastSpaceIndex = i;
                            if (!hydrData.substring(i + 1).contains(separator)) {
                                hydrName = hydrData.substring(lastSpaceIndex + 1);
                                actionsTableModel.addRow(PHASE_LOCALIZATION, actionsTableRowCount() + 1, locationCode.toUpperCase(), String.format(Localization.get("message.localization.hydrant"), hydrName));
                                continue goto_get_hydr_for_location;
                            }
                            ++i;
                        }
                        continue;
                    }
                    hydrName = hydrData.substring(1, hydrData.length() - 1);
                    actionsTableModel.addRow(PHASE_LOCALIZATION, actionsTableRowCount() + 1, locationCode.toUpperCase(), String.format(Localization.get("message.localization.hydrant"), hydrName));
                }
                if (!isolationRooms.isEmpty()) {
                    for (roomIndex = 1; roomIndex <= isolationRooms.length(); ++roomIndex)
                        actionsTableModel.addRow(PHASE_PREVENTION_OF_FIRE, actionsTableRowCount() + 1, isolationRooms.substring(roomIndex - 1, roomIndex).toUpperCase(), String.format(Localization.get("message.prevention.fire"), isolationRooms.substring(roomIndex - 1, roomIndex).toUpperCase()));
                }
                if (!isolationMechRooms.isEmpty()) {
                    for (roomIndex = 1; roomIndex <= isolationMechRooms.length(); ++roomIndex)
                        actionsTableModel.addRow(PHASE_MECHANICAL_DAMAGE, actionsTableRowCount() + 1, isolationMechRooms.substring(roomIndex - 1, roomIndex).toUpperCase(), String.format(Localization.get("message.prevention.mechanical"), isolationMechRooms.substring(roomIndex - 1, roomIndex).toUpperCase()));
                }
                if (!explosionRooms.isEmpty()) {
                    for (roomIndex = 1; roomIndex <= explosionRooms.length(); ++roomIndex)
                        actionsTableModel.addRow(PHASE_PREVENTION_OF_EXPLOSION, actionsTableRowCount() + 1, explosionRooms.substring(roomIndex - 1, roomIndex).toUpperCase(), String.format(Localization.get("message.prevention.explosion"), explosionRooms.substring(roomIndex - 1, roomIndex).toUpperCase()));
                }
            }
            showForcesArrangement();
        }
        if (treeNodeName.equals(TREE_FIRE_EXTINGUISHING_PLAN)) {
            String firePlanData = clips.eval("(collect-evac-accedent fire)").toString();
            firePlanRooms = firePlanData.substring(1, firePlanData.length() - 1);
            if (actionsTableRowCount() > 0) {
                for (int i = 0; i < actionsTableRowCount(); ++i)
                    actionsTableModel.removeRow();
            }
            for (int i = 1; i <= firePlanRooms.length(); ++i) {
                String roomCode = firePlanRooms.substring(i - 1, i);
                String planData = clips.eval("(get-plan-from " + roomCode + ")").toString();
                planData = planData.substring(1, planData.length() - 1);
                actionsTableModel.addRow(PHASE_FIRE_EXTINGUISHING, actionsTableRowCount() + 1, roomCode.toUpperCase(), String.format(Localization.get("message.extinguishing"), planData.toUpperCase()));
            }
            showFireExtinguishing();
        }
        actionsTableModel.fireTableDataChanged();
    }

    private int actionsTableRowCount() {
        return actionsTableModel.getRowCount();
    }

    public void paintFireLocation(String locationCode) {
        for (int i = 0; i < Main.locations_labels.length; ++i) {
            if (!locationCode.equals(Main.locations_labels[i].toLowerCase()))
                continue;
            ++index;
            fill_fire[index] = i;
        }
    }

    public void paintThreatLocation(String locationCode) {
        for (int i = 0; i < Main.locations_labels.length; ++i) {
            if (!locationCode.equals(Main.locations_labels[i].toLowerCase()))
                continue;
            ++indexthreat;
            fill_threat[indexthreat] = i;
        }
    }

    public void paintEvacuationLocation(String locationCode) {
        for (int i = 0; i < Main.locations_labels.length; ++i) {
            if (!locationCode.equals(Main.locations_labels[i].toLowerCase()))
                continue;
            ++indexevac;
            fill_evac[indexevac] = i;
        }
    }

    public void paintExplosionLocation(String locationCode) {
        for (int i = 0; i < expl_labels.length; ++i) {
            if (!locationCode.equals(expl_labels[i].toLowerCase()))
                continue;
            ++indexexpl;
            fill_expl[indexexpl] = i;
        }
    }

    public void paintFireLines(String locationCode) {
        for (int i = 0; i < Main.border_labels.length; ++i) {
            if (!locationCode.equals(Main.border_labels[i].toLowerCase()))
                continue;
            ++indexlines;
            fill_lines[indexlines] = i;
        }
    }

    public void put_buttons_evacuation(String locationCode) {
        for (int i = 0; i < Main.evac_labels.length; ++i) {
            if (!locationCode.equals(Main.evac_labels[i].toLowerCase()))
                continue;
            evac[i].setVisible(true);
            evac[i].setSelected(false);
            evac[i].setEnabled(true);
        }
        evacVisible = true;
    }

    public void put_buttons_air(String locationCode) {
        for (int i = 0; i < Main.ventil_labels.length; ++i) {
            if (!locationCode.equals(Main.ventil_labels[i].toLowerCase()))
                continue;
            ventil[i].setVisible(true);
            ventil[i].setSelected(false);
            ventil[i].setEnabled(true);
        }
        ventVisible = true;
    }

    public void put_buttons_doors(String locationCode, String keepOpen) {
        for (int i = 0; i < Main.door_labels.length; ++i) {
            if (!locationCode.equals(Main.door_labels[i].toLowerCase()))
                continue;
            dr[i].setVisible(true);
            dr[i].setSelected(false);
            dr[i].setEnabled(true);
            dr[i].setKeepOpen(keepOpen);
        }
        drVisible = true;
    }

    public void put_buttons_explosion(String locationCode) {
        for (int i = 0; i < expl_labels.length; ++i) {
            if (!locationCode.equals(expl_labels[i].toLowerCase()))
                continue;
            expl[i].setVisible(true);
            expl[i].setSelected(false);
            expl[i].setEnabled(true);
        }
        explVisible = true;
    }

    public void put_buttons_isolation(String locationCode) {
        for (int i = 0; i < Main.isol_labels.length; ++i) {
            if (!locationCode.equals(Main.isol_labels[i].toLowerCase()))
                continue;
            isol[i].setVisible(true);
            isol[i].setSelected(false);
            isol[i].setEnabled(true);
        }
        isolVisible = true;
    }

    public void put_buttons_isolation_mech(String locationCode) {
        for (int i = 0; i < Main.isol_mech_labels.length; ++i) {
            if (!locationCode.equals(Main.isol_mech_labels[i].toLowerCase()))
                continue;
            isol_mech[i].setVisible(true);
            isol_mech[i].setSelected(false);
            isol_mech[i].setEnabled(true);
        }
        isol_mechVisible = true;
    }

    public void put_labels_hydr_count(String locationCode, String hydrData, String hydrName) {
        for (int i = 0; i < Main.hydr_count_labels.length; ++i) {
            if (!locationCode.equals(Main.hydr_count_labels[i].toLowerCase()))
                continue;
            hydr_count[i].setVisible(true);
            hydr_count[i].setEnabled(true);
            hydr_count[i].setNumbers(hydrData, hydrName);
        }
        hydr_countVisible = true;
    }

    public void put_labels_ext_count(String locationCode, String hydrData, String hydrName) {
        for (int i = 0; i < Main.hydr_count_labels.length; ++i) {
            if (!locationCode.equals(Main.hydr_count_labels[i].toLowerCase()))
                continue;
            ext_count[i].setVisible(true);
            ext_count[i].setEnabled(true);
            ext_count[i].setLabels(hydrData, hydrName);
        }
        ext_countVisible = true;
    }

    public void put_labels_hydr(String hydrLabel, String hydrData) {
        for (int i = 0; i < Main.hydr_labels.length; ++i) {
            if (!hydrLabel.equals(Main.hydr_labels[i].toLowerCase()))
                continue;
            hydr[i].setVisible(true);
            hydr[i].setEnabled(true);
            hydr[i].setNumbers(hydrData, Main.hydr_labels[i]);
        }
        hydrVisible = true;
    }

    public void put_buttons_hydr_out(String locationCode, String hydrName) {
        for (int i = 0; i < Main.hydr_out_labels.length; ++i) {
            if (!hydrName.equals(Main.hydr_out_labels[i].toLowerCase()))
                continue;
            hydr_out[hydr_out_index] = new HydrOutButton(Main.hydr_out_labels[i]);
            jlab.add(hydr_out[hydr_out_index]);
            for (int j = 0; j < Main.hydr_loc_labels.length; ++j) {
                if (!locationCode.equals(Main.hydr_loc_labels[j].toLowerCase()))
                    continue;
                int hydr_loc = Main.hydrant_out_locations[j][1][0];
                int hydr_button_width = Main.hydrant_out_locations[j][2][0];
                int height = hydr_button_width == 1 ? 15 : 26;
                hydr_out[hydr_out_index].setBounds(Main.hydrant_out_locations[j][0][0], Main.hydrant_out_locations[j][0][1] + hydr_loc * 14, 66 / hydr_button_width + 1, height);
                hydr_out[hydr_out_index].setSize(hydr_button_width);
                hydr_out[hydr_out_index].setVisible(true);
                hydr_out[hydr_out_index].setEnabled(true);
                Main.hydrant_out_locations[j][1][0] = Main.hydrant_out_locations[j][1][0] + hydr_button_width;
            }
        }
        ++hydr_out_index;
        hydr_outVisible = true;
    }

    public void put_buttons_hydr_ext(String locationCode, String hydrName) {
        for (int i = 0; i < Main.hydr_out_labels.length; ++i) {
            if (!hydrName.equals(Main.hydr_out_labels[i].toLowerCase()))
                continue;
            hydr_ext[hydr_ext_index] = new HydrExtButton(Main.hydr_out_labels[i]);
            jlab.add(hydr_ext[hydr_ext_index]);
            for (int j = 0; j < Main.hydr_loc_labels.length; ++j) {
                if (!locationCode.equals(Main.hydr_loc_labels[j].toLowerCase()))
                    continue;
                int hydr_loc = Main.hydrant_ext_locations[j][1][0];
                int hydr_button_width = Main.hydrant_ext_locations[j][2][0];
                int height = hydr_button_width == 1 ? 15 : 26;
                hydr_ext[hydr_ext_index].setBounds(Main.hydrant_ext_locations[j][0][0], Main.hydrant_ext_locations[j][0][1] + hydr_loc * 14, 66 / hydr_button_width + 1, height);
                hydr_ext[hydr_ext_index].setSize(hydr_button_width);
                hydr_ext[hydr_ext_index].setVisible(true);
                hydr_ext[hydr_ext_index].setEnabled(true);
                Main.hydrant_ext_locations[j][1][0] = Main.hydrant_ext_locations[j][1][0] + hydr_button_width;
            }
        }
        ++hydr_ext_index;
        hydr_extVisible = true;
    }

    public void put_buttons_hydr_ext_b(String locationCode, String hydrName) {
        for (int i = 0; i < Main.hydr_out_labels.length; ++i) {
            if (!hydrName.equals(Main.hydr_out_labels[i].toLowerCase()))
                continue;
            hydr_ext_b[hydr_ext_b_index] = new HydrExtBorderToButton(Main.hydr_out_labels[i]);
            jlab.add(hydr_ext_b[hydr_ext_b_index]);
            for (int j = 0; j < Main.hydr_loc_labels.length; ++j) {
                if (!locationCode.equals(Main.hydr_loc_labels[j].toLowerCase()))
                    continue;
                int hydr_loc = Main.hydrant_ext_locations[j][1][0];
                int hydr_button_width = Main.hydrant_ext_locations[j][2][0];
                int height = hydr_button_width == 1 ? 15 : 26;
                hydr_ext_b[hydr_ext_b_index].setBounds(Main.hydrant_ext_locations[j][0][0], Main.hydrant_ext_locations[j][0][1] + hydr_loc * 14, 66 / hydr_button_width + 1, height);
                hydr_ext_b[hydr_ext_b_index].setSize(hydr_button_width);
                hydr_ext_b[hydr_ext_b_index].setVisible(true);
                hydr_ext_b[hydr_ext_b_index].setEnabled(true);
                Main.hydrant_ext_locations[j][1][0] = Main.hydrant_ext_locations[j][1][0] + hydr_button_width;
            }
        }
        ++hydr_ext_b_index;
        hydr_ext_bVisible = true;
    }

    public void put_buttons_hydr_ext_b_from(String locationCode, String hydrName) {
        for (int i = 0; i < Main.hydr_out_labels.length; ++i) {
            if (!hydrName.equals(Main.hydr_out_labels[i].toLowerCase()))
                continue;
            hydr_ext_b_from[hydr_ext_b_from_index] = new HydrExtBorderFromButton(Main.hydr_out_labels[i]);
            jlab.add(hydr_ext_b_from[hydr_ext_b_from_index]);
            for (int j = 0; j < Main.hydr_loc_labels.length; ++j) {
                if (!locationCode.equals(Main.hydr_loc_labels[j].toLowerCase()))
                    continue;
                int hydr_loc = Main.hydrant_out_locations[j][1][0];
                int hydr_button_width = Main.hydrant_out_locations[j][2][0];
                int width;
                int verticalOffset;
                if (hydr_button_width == 1) {
                    verticalOffset = 15;
                    width = 14;
                } else {
                    verticalOffset = 26;
                    width = 12;
                }
                hydr_ext_b_from[hydr_ext_b_from_index].setBounds(Main.hydrant_out_locations[j][0][0], Main.hydrant_out_locations[j][0][1] + hydr_loc * width, 66 / hydr_button_width + 1, verticalOffset);
                hydr_ext_b_from[hydr_ext_b_from_index].setSize(hydr_button_width);
                hydr_ext_b_from[hydr_ext_b_from_index].setVisible(true);
                hydr_ext_b_from[hydr_ext_b_from_index].setEnabled(true);
                Main.hydrant_out_locations[j][1][0] = Main.hydrant_out_locations[j][1][0] + hydr_button_width;
            }
        }
        ++hydr_ext_b_from_index;
        hydr_ext_b_fromVisible = true;
    }

    Main() {
        clips = new Environment();
        System.out.printf("CLIPS initialized: CLIPSJNI: %s, CLIPS: %s%n", Environment.getCLIPSJNIVersion(), Environment.getCLIPSVersion());

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        jfrm.setLayout(gridBagLayout);
        jfrm.setSize(1250, 810);
        jfrm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jfrm.setLocation(15, 87);
        jfrm.setBackground(Color.white);

        jppnl = new JPanel();
        jpnlCont = new JPanel();
        jlab = new JLabel(map);
        gbc.fill = 2;
        gbc.weightx = 1.0;
        gbc.gridwidth = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gridBagLayout.setConstraints(jppnl, gbc);
        gbc.fill = 1;
        gbc.weighty = 1.0;
        gbc.gridwidth = 1;
        gbc.anchor = 17;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gridBagLayout.setConstraints(jtreescrl, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gridBagLayout.setConstraints(jtblEventsTableScrl, gbc);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gridBagLayout.setConstraints(jtblActionsTableScrl, gbc);
        //================================================================
        // TREE (jtreescrl + jtree)
        // Левая панель с деревом фаз решения. Содержит JTree, узлы дерева (subtree*),
        // методы expandAll, настройку слушателя выбора (TreeSelectionListener).
        // Связанные элементы: tree, jtreescrl, subtree1..6, анонимный TreeSelectionListener ($1).
        //================================================================
        jfrm.add(jtreescrl);
        tree.add(subtree1);
        subtree1.add(subtree2);
        subtree1.add(subtree3);
        tree.add(subtree5);
        subtree5.add(subtree4);
        tree.add(subtree6);
        expandAll(jtree, true);
        jtree.setEditable(false);
        jtree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        jtree.setMinimumSize(new Dimension(400, 150));

        jtree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                updateActionsTable();
            }
        });

        //================================================================
        // MAP PANEL (jppnl + jpnlCont + paint overlays + jlab)
        // Главная панель с планом палубы (картинка map.gif). Содержит цепочку вложенных
        // JPanel для оверлеев (pntEvac -> pntExpl -> pntLines) и JLabel jlab,
        // на который добавляются все кнопки с абсолютным позиционированием (setBounds).
        // Связанные элементы: jppnl, jpnlCont, pntEvac, pntExpl, pntLines, jlab,
        // все массивы кнопок (sens, fire_btn, evac, ventil, dr, expl, isol и т.д.),
        // методы put_buttons_*, show*, paint*, и все inner классы *Button + Paint*.
        //================================================================
        jfrm.add(jppnl);
        jppnl.setBackground(Color.white);
        jppnl.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        jppnl.setMinimumSize(new Dimension(1226, 630));
        jppnl.setMaximumSize(new Dimension(1500, 650));
        jppnl.add(jpnlCont);
        jpnlCont.setOpaque(false);
        jpnlCont.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        jpnlCont.add(pntEvac);
        pntEvac.setOpaque(false);
        pntEvac.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        pntEvac.add(pntExpl);
        pntExpl.setOpaque(false);
        pntExpl.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        pntExpl.add(pntLines);
        pntLines.setOpaque(false);
        pntLines.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        pntLines.add(jlab);
        jlab.setOpaque(false);
        jlab.setBackground(Color.white);
        jlab.setVerticalTextPosition(1);

        //----------------------------------------------------------------
        // Кнопки на карте (добавляются на jlab с абсолютным позиционированием)
        // Все массивы кнопок (sens, fire_btn и т.д.), их создание, setBounds,
        // setVisible/enabled, и связанные put_/show_ методы.
        //----------------------------------------------------------------
        for (int n = 0; n < Main.sensors.length; ++n) {
            jlab.add(sens[n]);
            sens[n].setBounds(Main.sensors[n][0], Main.sensors[n][1], 19, 19);
        }
        for (int n = 0; n < fire_btn.length; ++n) {
            jlab.add(fire_btn[n]);
            fire_btn[n].setBounds(Main.fire_button[n][0], Main.fire_button[n][1], 11, 11);
        }
        for (int n = 0; n < Main.evacuations.length; ++n) {
            jlab.add(evac[n]);
            evac[n].setBounds(Main.evacuations[n][0], Main.evacuations[n][1], 19, 19);
            evac[n].setVisible(false);
            evac[n].setEnabled(false);
        }
        for (int n = 0; n < Main.ventilations.length; ++n) {
            jlab.add(ventil[n]);
            ventil[n].setBounds(Main.ventilations[n][0], Main.ventilations[n][1], 19, 19);
            ventil[n].setVisible(false);
            ventil[n].setEnabled(false);
        }
        for (int n = 0; n < Main.doors.length; ++n) {
            jlab.add(dr[n]);
            dr[n].setBounds(Main.doors[n][0], Main.doors[n][1], Main.doors[n][2], Main.doors[n][3]);
            dr[n].setVisible(false);
            dr[n].setEnabled(false);
        }
        for (int n = 0; n < explosions.length; ++n) {
            jlab.add(expl[n]);
            expl[n].setBounds(explosions[n][0], explosions[n][1], 19, 19);
            expl[n].setVisible(false);
            expl[n].setEnabled(false);
        }
        for (int n = 0; n < Main.isolations.length; ++n) {
            jlab.add(isol[n]);
            isol[n].setBounds(Main.isolations[n][0], Main.isolations[n][1], 19, 19);
            isol[n].setVisible(false);
            isol[n].setEnabled(false);
        }
        for (int n = 0; n < Main.isolations_mech.length; ++n) {
            jlab.add(isol_mech[n]);
            isol_mech[n].setBounds(Main.isolations_mech[n][0], Main.isolations_mech[n][1], 19, 19);
            isol_mech[n].setVisible(false);
            isol_mech[n].setEnabled(false);
        }
        for (int n = 0; n < Main.hydrants_count.length; ++n) {
            jlab.add(hydr_count[n]);
            if (hydr_count[n].getLabelSize().equals("full")) {
                hydr_count[n].setBounds(Main.hydrants_count[n][0], Main.hydrants_count[n][1], 67, 18);
            } else {
                hydr_count[n].setBounds(Main.hydrants_count[n][0], Main.hydrants_count[n][1], 34, 18);
            }
            hydr_count[n].setVisible(false);
            hydr_count[n].setEnabled(false);
        }
        for (int n = 0; n < Main.hydrants.length; ++n) {
            jlab.add(hydr[n]);
            hydr[n].setBounds(Main.hydrants[n][0], Main.hydrants[n][1], hydr[n].getLabelSize() * 14 + 3, 31);
            hydr[n].setVisible(false);
            hydr[n].setEnabled(false);
        }
        for (int n = 0; n < Main.hydrants_ext.length; ++n) {
            jlab.add(ext_count[n]);
            if (ext_count[n].getLabelSize().equals("full")) {
                ext_count[n].setBounds(Main.hydrants_ext[n][0], Main.hydrants_ext[n][1], 67, 18);
            } else {
                ext_count[n].setBounds(Main.hydrants_ext[n][0], Main.hydrants_ext[n][1], 34, 18);
            }
            ext_count[n].setVisible(false);
            ext_count[n].setEnabled(false);
        }
        //================================================================
        // FIRE EVENTS TABLE (jtblEventsTableScrl + jtblEventsTable + EventsTableModel + jpnlEventsTable)
        // Верхняя таблица оповещений о возгораниях (срабатывания датчиков, нажатия кнопок и т.д.).
        // Содержит модель, скролл, панель jpnlEventsTable, настройку колонок.
        // Связанные элементы: jtblEventsTable, eventsTableModel, jtblEventsTableScrl, jpnlEventsTable.
        //================================================================
        jfrm.add(jtblEventsTableScrl);
        jtblEventsTableScrl.setMinimumSize(new Dimension(120, 150));
        jtblEventsTableScrl.setMaximumSize(new Dimension(290, 200));
        jtblEventsTable.setSelectionMode(0);
        jtblEventsTable.getColumnModel().getColumn(0).setMaxWidth(20);
        jtblEventsTable.getColumnModel().getColumn(1).setWidth(140);
        jtblEventsTable.getColumnModel().getColumn(2).setMaxWidth(35);
        jtblEventsTable.getColumnModel().getColumn(3).setMaxWidth(60);
        jtblEventsTable.getColumnModel().getColumn(4).setMaxWidth(60);

        //================================================================
        // RECOMMENDED ACTIONS TABLE (jtblActionsTableScrl + jtblActionsTable + ActionsTableModel + jpnlActionsTable)
        // Нижняя таблица рекомендуемых действий для тушения пожара. Содержит модель, скролл,
        // панель jpnlActionsTable, слушатель мыши для popup с объяснениями (MouseAdapter $2),
        // логику заполнения на основе фаз из CLIPS.
        // Связанные элементы: jtblActionsTable, actionsTableModel, jtblActionsTableScrl, jpnlActionsTable, анонимный MouseAdapter.
        //================================================================
        jfrm.add(jtblActionsTableScrl);
        jtblActionsTableScrl.setMinimumSize(new Dimension(280, 150));
        jtblActionsTableScrl.setMaximumSize(new Dimension(520, 200));
        jtblActionsTable.setSelectionMode(0);
        jtblActionsTable.getColumnModel().getColumn(0).setMinWidth(60);
        jtblActionsTable.getColumnModel().getColumn(1).setMaxWidth(20);
        jtblActionsTable.getColumnModel().getColumn(2).setMaxWidth(35);
        jtblActionsTable.getColumnModel().getColumn(3).setMinWidth(370);

        //----------------------------------------------------------------
        // Листенеры для RECOMMENDED ACTIONS TABLE
        // Анонимный MouseAdapter для показа popup с объяснениями из CLIPS.
        //----------------------------------------------------------------
        jtblActionsTable.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                int selectedRow = jtblActionsTable.getSelectedRow();
                Rectangle rectangle = jtblActionsTable.getCellRect(selectedRow, 3, true);
                String explanationPrevious1 = null;
                String explanationPrevious2 = null;
                String explanationConsequent = null;
                String phase = jtblActionsTable.getValueAt(selectedRow, 0).toString();
                String roomCode = jtblActionsTable.getValueAt(selectedRow, 2).toString().toLowerCase();
                if (phase.equals(PHASE_GERMETISATION)) {
                    if (roomCode.length() == 1) {
                        explanationPrevious1 = clips.eval("(get-explanation antec1 " + roomCode + " )").toString();
                        explanationPrevious2 = clips.eval("(get-explanation antec2 " + roomCode + " )").toString();
                        explanationConsequent = clips.eval("(get-explanation consec " + roomCode + " )").toString();
                    }
                    if (roomCode.length() == 4) {
                        explanationPrevious1 = clips.eval("(get-explanation2 antec1 " + roomCode.substring(0, 1).toLowerCase() + " " + roomCode.substring(3, 4).toLowerCase() + " )").toString();
                        explanationPrevious2 = clips.eval("(get-explanation2 antec2 " + roomCode.substring(0, 1).toLowerCase() + " " + roomCode.substring(3, 4).toLowerCase() + " )").toString();
                        explanationConsequent = clips.eval("(get-explanation2 consec " + roomCode.substring(0, 1).toLowerCase() + " " + roomCode.substring(3, 4).toLowerCase() + " )").toString();
                    }
                }
                if (phase.equals(PHASE_EVACUATION)) {
                    explanationPrevious1 = clips.eval("(get-explanation-evac antec1 " + roomCode + " )").toString();
                    explanationPrevious2 = clips.eval("(get-explanation-evac antec2 " + roomCode + " )").toString();
                    explanationConsequent = clips.eval("(get-explanation-evac consec " + roomCode + " )").toString();
                }
                if (phase.equals(PHASE_PREVENTION_OF_EXPLOSION)) {
                    explanationPrevious1 = clips.eval("(get-explanation-expl antec1 " + roomCode + " )").toString();
                    explanationPrevious2 = clips.eval("(get-explanation-expl antec2 " + roomCode + " )").toString();
                    explanationConsequent = clips.eval("(get-explanation-expl consec " + roomCode + " )").toString();
                }
                if (phase.equals(PHASE_PREVENTION_OF_FIRE)) {
                    explanationPrevious1 = clips.eval("(get-explanation-isol antec1 " + roomCode + ")").toString();
                    explanationPrevious2 = clips.eval("(get-explanation-isol antec2 " + roomCode + ")").toString();
                    explanationConsequent = clips.eval("(get-explanation-isol consec " + roomCode + ")").toString();
                }
                if (phase.equals(PHASE_MECHANICAL_DAMAGE)) {
                    explanationPrevious1 = clips.eval("(get-explanation-isol-mech antec1 " + roomCode + ")").toString();
                    explanationPrevious2 = clips.eval("(get-explanation-isol-mech antec2 " + roomCode + ")").toString();
                    explanationConsequent = clips.eval("(get-explanation-isol-mech consec " + roomCode + ")").toString();
                }
                jpu.removeAll();
                jpu.add(new JMenuItem(String.format("<html>%s<br>%s<hr>%s</html>", explanationPrevious1.substring(1, explanationPrevious1.length() - 1), explanationPrevious2.substring(1, explanationPrevious2.length() - 1), explanationConsequent.substring(1, explanationConsequent.length() - 1))));
                jpu.show(jtblActionsTable, rectangle.x, rectangle.y);
            }
        });
        for (int n = 0; n < Main.sensors.length; ++n) {
            sens[n].addActionListener(this);
            sens[n].setActionCommand(ELEMENT_SENSOR + Main.sensor_labels[n]);
        }
        for (int n = 0; n < Main.fire_button.length; ++n) {
            fire_btn[n].addActionListener(this);
            fire_btn[n].setActionCommand(ELEMENT_BUTTON + Main.fire_labels[n]);
        }
        for (int n = 0; n < Main.evacuations.length; ++n) {
            evac[n].addActionListener(this);
            evac[n].setActionCommand(ELEMENT_EVACUATION + Main.evac_labels[n]);
        }
        for (int n = 0; n < Main.ventilations.length; ++n) {
            ventil[n].addActionListener(this);
            ventil[n].setActionCommand(ELEMENT_VENTILATION + Main.ventil_labels[n]);
        }
        for (int n = 0; n < Main.doors.length; ++n) {
            dr[n].addActionListener(this);
            dr[n].setActionCommand(ELEMENT_DOOR + Main.door_labels[n]);
        }
        for (int n = 0; n < explosions.length; ++n) {
            expl[n].addActionListener(this);
            expl[n].setActionCommand(ELEMENT_EXPLOSIVE + expl_labels[n] + expl_object_labels[n]);
        }
        for (int n = 0; n < Main.isolations.length; ++n) {
            isol[n].addActionListener(this);
            isol[n].setActionCommand(ELEMENT_ISOLATION + Main.isol_labels[n] + Main.isol_object_labels[n]);
        }
        for (int n = 0; n < Main.isolations_mech.length; ++n) {
            isol_mech[n].addActionListener(this);
            isol_mech[n].setActionCommand(ELEMENT_ISOLATION + Main.isol_mech_labels[n] + ELEMENT_ISOLATION_MECH);
        }
        clips.load(CLIPS_RULES_BASE);
        clips.reset();
        jfrm.setVisible(true);
    }

    public void expandAll(JTree jTree, boolean expand) {
        TreeNode treeNode = (TreeNode)jTree.getModel().getRoot();
        expandAll(jTree, new TreePath(treeNode), expand);
    }

    private void expandAll(JTree jTree, TreePath treePath, boolean expand) {
        TreeNode treeNode = (TreeNode)treePath.getLastPathComponent();
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

    public void actionPerformed(ActionEvent actionEvent) {
        String command = actionEvent.getActionCommand();
        if (command.startsWith(ELEMENT_SENSOR) || command.startsWith(ELEMENT_BUTTON)) {
            int n;
            int n2;
            int n3;
            String clipsResult;
            String locationCode;
            String hydrData;
            String hydrName;
            String separator;
            String roomList;
            String germData;
            String keepOpenData;
            String isolationData;
            String mechData;
            String explosionData;
            String fireLineData;
            String firePlanData;
            String graphData;
            pressed = command.substring(2, 3).toLowerCase();
            clips.eval("(send [" + pressed + "] put-accedent fire)");
            clips.run();
            clipsResult = clips.eval("(collect-evac-accedent fire)").toString();
            fire = clipsResult.substring(1, clipsResult.length() - 1);
            index = -1;
            for (int roomIndex = 1; roomIndex <= fire.length(); ++roomIndex) {
                locationCode = fire.substring(roomIndex - 1, roomIndex);
                paintFireLocation(locationCode);
            }
            clipsResult = clips.eval("(collect-evac-accedent threat)").toString();
            threat = clipsResult.substring(1, clipsResult.length() - 1);
            indexthreat = -1;
            for (int roomIndex = 1; roomIndex <= threat.length(); ++roomIndex) {
                locationCode = threat.substring(roomIndex - 1, roomIndex);
                paintThreatLocation(locationCode);
            }
            clipsResult = clips.eval("(collect-evac-evacuation to-evacuate)").toString();
            evacuate = clipsResult.substring(1, clipsResult.length() - 1);
            indexevac = -1;
            for (int roomIndex = 1; roomIndex <= evacuate.length(); ++roomIndex) {
                locationCode = evacuate.substring(roomIndex - 1, roomIndex);
                put_buttons_evacuation(locationCode);
                paintEvacuationLocation(locationCode);
            }
            roomList = (clipsResult = clips.eval("(collect-germ-loc to-off)").toString()).toLowerCase().substring(1, clipsResult.length() - 1);
            for (int i = 1; i <= roomList.length(); ++i) {
                locationCode = roomList.substring(i - 1, i);
                put_buttons_air(locationCode);
            }
            germData = (clipsResult = clips.eval("(collect-germ-door to-close)").toString()).substring(1, clipsResult.length() - 1);
            for (int i = 0; i <= germData.length() / 2 - 1; ++i) {
                locationCode = germData.substring(i * 2, i * 2 + 2);
                put_buttons_doors(locationCode, "no");
            }
            keepOpenData = (clipsResult = clips.eval("(collect-germ-door keep-open)").toString()).substring(1, clipsResult.length() - 1);
            for (int i = 0; i <= keepOpenData.length() / 2 - 1; ++i) {
                locationCode = keepOpenData.substring(i * 2, i * 2 + 2);
                put_buttons_doors(locationCode, "yes");
            }
            clipsResult = clips.eval("(collect-action-phase explosion)").toString();
            explosionData = clipsResult.toLowerCase().substring(1, clipsResult.length() - 1);
            indexexpl = -1;
            for (int i = 1; i <= explosionData.length(); ++i) {
                locationCode = explosionData.substring(i - 1, i);
                put_buttons_explosion(locationCode);
                paintExplosionLocation(locationCode);
            }
            isolationData = (clipsResult = clips.eval("(collect-action-phase isolation)").toString()).toLowerCase().substring(1, clipsResult.length() - 1);
            for (int i = 1; i <= isolationData.length(); ++i) {
                locationCode = isolationData.substring(i - 1, i);
                put_buttons_isolation(locationCode);
            }
            mechData = (clipsResult = clips.eval("(collect-isol-mech stop)").toString()).toLowerCase().substring(1, clipsResult.length() - 1);
            for (int i = 1; i <= mechData.length(); ++i) {
                locationCode = mechData.substring(i - 1, i);
                put_buttons_isolation_mech(locationCode);
            }
            clipsResult = clips.eval("(get-line1-borders)").toString();
            indexlines = -1;
            fireLineData = clipsResult.substring(1, clipsResult.length() - 1);
            for (int i = 0; i <= fireLineData.length() / 2 - 1; ++i) {
                locationCode = fireLineData.substring(i * 2, i * 2 + 2);
                paintFireLines(locationCode);
            }
            clipsResult = clips.eval("(get-fire-line-locations)").toString();
            roomList = clipsResult.toLowerCase().substring(1, clipsResult.length() - 1);
            for (n3 = 0; n3 < Main.hydr_count_labels.length; ++n3) {
                hydr_count[n3].setVisible(false);
                hydr_count[n3].setEnabled(false);
            }
            for (n3 = 1; n3 <= roomList.length(); ++n3) {
                locationCode = roomList.substring(n3 - 1, n3);
                hydrData = clips.eval("(get-fire-line-hydr " + locationCode + " hydrants-here)").toString();
                hydrName = clips.eval("(get-fire-line-hydr " + locationCode + " hydrants-need)").toString();
                put_labels_hydr_count(locationCode, hydrData, hydrName);
            }
            for (n3 = 0; n3 < Main.hydr_labels.length; ++n3) {
                hydrData = clips.eval("(get-hydrant-outs " + Main.hydr_labels[n3] + ")").toString();
                put_labels_hydr(Main.hydr_labels[n3], hydrData);
            }
            if (hydr_out_index != 0) {
                for (n3 = 0; n3 < hydr_out_index; ++n3)
                    jlab.remove(hydr_out[n3]);
                for (n3 = 0; n3 < Main.hydrant_out_locations.length; ++n3)
                    Main.hydrant_out_locations[n3][1][0] = 0;
                hydr_out_index = 0;
            }
            goto_get_hydr_for_location: for (n3 = 1; n3 <= roomList.length(); ++n3) {
                locationCode = roomList.substring(n3 - 1, n3);
                hydrData = clips.eval("(get-hydr-for-location " + locationCode + ")").toString();
                if (hydrData.equals("()"))
                    continue;
                separator = " ";
                n2 = -1;
                if (hydrData.contains(separator)) {
                    hydrData = hydrData.substring(1, hydrData.length() - 1);
                    for (n = 0; n < hydrData.length(); ++n) {
                        if (hydrData.charAt(n) != ' ')
                            continue;
                        hydrName = hydrData.substring(n2 + 1, n);
                        put_buttons_hydr_out(locationCode, hydrName);
                        n2 = n;
                        if (!hydrData.substring(n + 1).contains(separator)) {
                            hydrName = hydrData.substring(n2 + 1);
                            put_buttons_hydr_out(locationCode, hydrName);
                            continue goto_get_hydr_for_location;
                        }
                        ++n;
                    }
                    continue;
                }
                hydrName = hydrData.substring(1, hydrData.length() - 1);
                put_buttons_hydr_out(locationCode, hydrName);
            }
            for (n3 = 1; n3 <= fire.length(); ++n3) {
                locationCode = fire.substring(n3 - 1, n3);
                hydrData = clips.eval("(get-plan-from " + locationCode + ")").toString();
                hydrName = clips.eval("(get-plan-number " + locationCode + ")").toString();
                put_labels_ext_count(locationCode, hydrData, hydrName);
            }
            if (hydr_ext_b_index != 0) {
                for (n3 = 0; n3 < hydr_ext_b_index; ++n3)
                    jlab.remove(hydr_ext_b[n3]);
                for (n3 = 0; n3 < Main.hydrant_out_locations.length; ++n3)
                    Main.hydrant_out_locations[n3][1][0] = 0;
                hydr_ext_b_index = 0;
            }
            goto_get_ext_b_to_for_location: for (n3 = 1; n3 <= fire.length(); ++n3) {
                locationCode = fire.substring(n3 - 1, n3);
                hydrData = clips.eval("(get-ext-b-to-for-location " + locationCode + ")").toString();
                if (hydrData.equals("()"))
                    continue;
                separator = " ";
                n2 = -1;
                if (hydrData.contains(separator)) {
                    hydrData = hydrData.substring(1, hydrData.length() - 1);
                    for (n = 0; n < hydrData.length(); ++n) {
                        if (hydrData.charAt(n) != ' ')
                            continue;
                        hydrName = hydrData.substring(n2 + 1, n);
                        put_buttons_hydr_ext_b(locationCode, hydrName);
                        n2 = n;
                        if (!hydrData.substring(n + 1).contains(separator)) {
                            hydrName = hydrData.substring(n2 + 1);
                            put_buttons_hydr_ext_b(locationCode, hydrName);
                            continue goto_get_ext_b_to_for_location;
                        }
                        ++n;
                    }
                    continue;
                }
                hydrName = hydrData.substring(1, hydrData.length() - 1);
                put_buttons_hydr_ext_b(locationCode, hydrName);
            }
            if (hydr_ext_index != 0) {
                for (n3 = 0; n3 < hydr_ext_index; ++n3)
                    jlab.remove(hydr_ext[n3]);
                for (n3 = 0; n3 < Main.hydrant_out_locations.length; ++n3)
                    Main.hydrant_out_locations[n3][1][0] = 0;
                hydr_ext_index = 0;
            }
            goto_get_ext_for_location: for (n3 = 1; n3 <= fire.length(); ++n3) {
                locationCode = fire.substring(n3 - 1, n3);
                hydrData = clips.eval("(get-ext-for-location " + locationCode + ")").toString();
                if (hydrData.equals("()"))
                    continue;
                separator = " ";
                n2 = -1;
                if (hydrData.contains(separator)) {
                    hydrData = hydrData.substring(1, hydrData.length() - 1);
                    for (n = 0; n < hydrData.length(); ++n) {
                        if (hydrData.charAt(n) != ' ')
                            continue;
                        hydrName = hydrData.substring(n2 + 1, n);
                        put_buttons_hydr_ext(locationCode, hydrName);
                        n2 = n;
                        if (!hydrData.substring(n + 1).contains(separator)) {
                            hydrName = hydrData.substring(n2 + 1);
                            put_buttons_hydr_ext(locationCode, hydrName);
                            continue goto_get_ext_for_location;
                        }
                        ++n;
                    }
                    continue;
                }
                hydrName = hydrData.substring(1, hydrData.length() - 1);
                put_buttons_hydr_ext(locationCode, hydrName);
            }
            graphData = (clipsResult = clips.eval("(get-graph-from-locations)").toString()).toLowerCase().substring(1, clipsResult.length() - 1);
            if (hydr_ext_b_from_index != 0) {
                for (int componentIndex = 0; componentIndex < hydr_ext_b_from_index; ++componentIndex)
                    jlab.remove(hydr_ext_b_from[componentIndex]);
                for (int componentIndex = 0; componentIndex < Main.hydrant_out_locations.length; ++componentIndex) {
                    Main.hydrant_out_locations[componentIndex][1][0] = 0;
                }
                hydr_ext_b_from_index = 0;
            }
            goto_get_ext_b_from_for_location: for (int i = 1; i <= graphData.length(); ++i) {
                locationCode = graphData.substring(i - 1, i);
                hydrData = clips.eval("(get-ext-b-from-for-location " + locationCode + ")").toString();
                if (hydrData.equals("()"))
                    continue;
                separator = " ";
                n = -1;
                if (hydrData.contains(separator)) {
                    hydrData = hydrData.substring(1, hydrData.length() - 1);
                    for (int j = 0; j < hydrData.length(); ++j) {
                        if (hydrData.charAt(j) != ' ')
                            continue;
                        hydrName = hydrData.substring(n + 1, j);
                        put_buttons_hydr_ext_b_from(locationCode, hydrName);
                        n = j;
                        if (!hydrData.substring(j + 1).contains(separator)) {
                            hydrName = hydrData.substring(n + 1);
                            put_buttons_hydr_ext_b_from(locationCode, hydrName);
                            continue goto_get_ext_b_from_for_location;
                        }
                        ++j;
                    }
                    continue;
                }
                hydrName = hydrData.substring(1, hydrData.length() - 1);
                put_buttons_hydr_ext_b_from(locationCode, hydrName);
            }
            addEventsTable(pressed);
            jtree.setSelectionRow(0);
        }
        if (command.startsWith(ELEMENT_EVACUATION)) {
            pressed = command.substring(2, 3);
            for (int i = 0; i < Main.evacuations.length; ++i) {
                if (!pressed.equals(Main.evac_labels[i]))
                    continue;
                if (evac[i].isSelected()) {
                    clips.eval("(send [" + Main.evac_labels[i].toLowerCase() + "] put-evacuation done)");
                    evac[i].setEnabled(false);
                    continue;
                }
                clips.eval("(send [" + Main.evac_labels[i].toLowerCase() + "] put-evacuation none)");
            }
            clips.eval("(focus MAIN)");
            clips.eval("(focus IMMEDIATE-EVACUATION)");
            clips.run();
            String evacData = clips.eval("(collect-evac-evacuation to-evacuate)").toString();
            evacuate = evacData.substring(1, evacData.length() - 1);
            indexevac = -1;
            for (int i = 1; i <= evacuate.length(); ++i) {
                String room = evacuate.substring(i - 1, i);
                paintEvacuationLocation(room);
            }
        }
        if (command.startsWith(ELEMENT_VENTILATION)) {
            pressed = command.substring(2, 3);
            for (int i = 0; i < Main.ventilations.length; ++i) {
                if (!pressed.equals(Main.ventil_labels[i]))
                    continue;
                if (ventil[i].isSelected()) {
                    clips.eval("(send [" + Main.ventil_labels[i].toLowerCase() + "] put-ventil off)");
                    continue;
                }
                clips.eval("(send [" + Main.ventil_labels[i].toLowerCase() + "] put-ventil on)");
            }
            clips.eval("(focus MAIN)");
            clips.eval("(focus IMMEDIATE-GERMETISATION)");
            clips.run();
        }
        if (command.startsWith(ELEMENT_DOOR)) {
            String door_label = command.substring(2, 4);
            for (int i = 0; i < Main.doors.length; ++i) {
                if (!door_label.equals(Main.door_labels[i]))
                    continue;
                if (dr[i].isSelected()) {
                    clips.eval("(send [door_" + Main.door_labels[i].substring(0, 1).toLowerCase() + "_to_" + Main.door_labels[i].substring(1, 2).toLowerCase() + "] put-status close)");
                    continue;
                }
                clips.eval("(send [door_" + Main.door_labels[i].substring(0, 1).toLowerCase() + "_to_" + Main.door_labels[i].substring(1, 2).toLowerCase() + "] put-status open)");
            }
            clips.eval("(focus MAIN)");
            clips.eval("(focus IMMEDIATE-GERMETISATION)");
            clips.run();
        }
        if (command.startsWith(ELEMENT_EXPLOSIVE)) {
            pressed = command.substring(2, 3);
            String objectType = command.substring(3, 6);
            for (int i = 0; i < explosions.length; ++i) {
                if (!pressed.equals(expl_labels[i]))
                    continue;
                if (expl[i].isSelected()) {
                    clips.eval("(action-edit explosion " + expl_labels[i].toLowerCase() + " done)");
                    continue;
                }
                if (objectType.equals(EXPLOSIVE_TYPE_AIR)) {
                    clips.eval("(action-edit explosion " + expl_labels[i].toLowerCase() + " carry_out)");
                }
                if (objectType.equals(EXPLOSIVE_TYPE_OIL)) {
                    clips.eval("(action-edit explosion " + expl_labels[i].toLowerCase() + " pump_out)");
                }
                if (!objectType.equals(EXPLOSIVE_TYPE_OTHER))
                    continue;
                clips.eval("(action-edit explosion " + expl_labels[i].toLowerCase() + " to_fight)");
            }
            String explosionDataStr = clips.eval("(collect-action-phase explosion)").toString();
            String explosionDataLocal = explosionDataStr.toLowerCase().substring(1, explosionDataStr.length() - 1);
            indexexpl = -1;
            for (int i = 1; i <= explosionDataLocal.length(); ++i) {
                String loc = explosionDataLocal.substring(i - 1, i);
                paintExplosionLocation(loc);
            }
            clips.eval("(focus MAIN)");
            clips.eval("(focus IMMEDIATE-EXPLOSION)");
            clips.run();
        }
        if (command.startsWith(ELEMENT_ISOLATION)) {
            pressed = command.substring(2, 3);
            String objectType = command.substring(3, 6);
            if (objectType.equals(ELEMENT_ISOLATION_MECH)) {
                for (int i = 0; i < Main.isolations_mech.length; ++i) {
                    if (!pressed.equals(Main.isol_mech_labels[i]))
                        continue;
                    if (isol_mech[i].isSelected()) {
                        clips.eval("(send [" + Main.isol_mech_labels[i].toLowerCase() + "] put-machinery done)");
                        continue;
                    }
                    clips.eval("(send [" + Main.isol_mech_labels[i].toLowerCase() + "] put-machinery stop)");
                }
            } else {
                for (int i = 0; i < Main.isolations.length; ++i) {
                    if (!pressed.equals(Main.isol_labels[i]))
                        continue;
                    if (isol[i].isSelected()) {
                        clips.eval("(action-edit isolation " + Main.isol_labels[i].toLowerCase() + " done)");
                        continue;
                    }
                    if (objectType.equals(EXPLOSIVE_TYPE_OIL)) {
                        clips.eval("(action-edit isolation " + Main.isol_labels[i].toLowerCase() + " pump_out)");
                    }
                    if (!objectType.equals("cls"))
                        continue;
                    clips.eval("(action-edit isolation " + Main.isol_labels[i].toLowerCase() + " carry_out)");
                }
            }
            clips.eval("(focus MAIN)");
            clips.eval("(focus IMMEDIATE-ISOLATION)");
            clips.run();
        }
        updateActionsTable();
        pntEvac.repaint();
        pntExpl.repaint();
        pntLines.repaint();
    }

    public void addEventsTable(String roomCode) {
        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(Calendar.getInstance().getTime());
        eventsTableModel.addRow(eventsTableModel.getRowCount() + 1, timestamp, roomCode.toUpperCase(), Localization.get("label.deck.upper"), Localization.get("label.compartment.third"), Localization.get("event.fire"));
        eventsTableModel.fireTableDataChanged();
    }

    public void showAll() {
        for (FireSensorButton sensor : sens)
            sensor.setVisible(true);
        sensVisible = true;
        for (FireButton button : fire_btn)
            button.setVisible(true);
        fireVisible = true;
        if (evacVisible) {
            for (EvacuateButton button : evac)
                button.setVisible(false);
            evacVisible = false;
        }
        if (ventVisible) {
            for (AirIsolButton button : ventil)
                button.setVisible(false);
            ventVisible = false;
        }
        if (drVisible) {
            for (DoorIsolButton button : dr)
                button.setVisible(false);
            drVisible = false;
        }
        if (explVisible) {
            for (ExplosiveButton button : expl)
                button.setVisible(false);
            explVisible = false;
        }
        if (isolVisible) {
            for (IsolationButton button : isol)
                button.setVisible(false);
            isolVisible = false;
        }
        if (isol_mechVisible) {
            for (IsolationButton button : isol_mech)
                button.setVisible(false);
            isol_mechVisible = false;
        }
        if (hydr_countVisible) {
            for (HydrCountButton button : hydr_count)
                button.setVisible(false);
            hydr_countVisible = false;
        }
        if (hydrVisible) {
            for (HydrButton button : hydr)
                button.setVisible(false);
            hydrVisible = false;
        }
        if (hydr_outVisible) {
            if (hydr_out_index != 0) {
                for (int i = 0; i < hydr_out_index; ++i)
                    hydr_out[i].setVisible(false);
            }
            hydr_outVisible = false;
        }
        if (ext_countVisible) {
            for (ExtCountButton button : ext_count)
                button.setVisible(false);
            ext_countVisible = false;
        }
        if (hydr_extVisible) {
            if (hydr_ext_index != 0) {
                for (int i = 0; i < hydr_ext_index; ++i)
                    hydr_ext[i].setVisible(false);
            }
            hydr_extVisible = false;
        }
        if (hydr_ext_bVisible) {
            if (hydr_ext_b_index != 0) {
                for (int i = 0; i < hydr_ext_b_index; ++i)
                    hydr_ext_b[i].setVisible(false);
            }
            hydr_ext_bVisible = false;
        }
        if (hydr_ext_b_fromVisible) {
            if (hydr_ext_b_from_index != 0) {
                for (int i = 0; i < hydr_ext_b_from_index; ++i)
                    hydr_ext_b_from[i].setVisible(false);
            }
            hydr_ext_b_fromVisible = false;
        }
    }

    public void showFirstTime() {
        for (EvacuateButton button : evac) {
            if (!button.isEnabled())
                continue;
            button.setVisible(true);
        }
        evacVisible = true;
        for (AirIsolButton button : ventil) {
            if (!button.isEnabled())
                continue;
            button.setVisible(true);
        }
        ventVisible = true;
        for (DoorIsolButton button : dr) {
            if (!button.isEnabled())
                continue;
            button.setVisible(true);
        }
        drVisible = true;
        if (sensVisible) {
            for (FireSensorButton sensor : sens) {
                if (sensor.isSelected())
                    continue;
                sensor.setVisible(false);
            }
            sensVisible = false;
        }
        if (fireVisible) {
            for (FireButton button : fire_btn) {
                if (button.isSelected())
                    continue;
                button.setVisible(false);
            }
            fireVisible = false;
        }
        if (explVisible) {
            for (ExplosiveButton button : expl)
                button.setVisible(false);
            explVisible = false;
        }
        if (isolVisible) {
            for (IsolationButton button : isol)
                button.setVisible(false);
            isolVisible = false;
        }
        if (isol_mechVisible) {
            for (IsolationButton button : isol_mech)
                button.setVisible(false);
            isol_mechVisible = false;
        }
        if (hydr_countVisible) {
            for (HydrCountButton button : hydr_count)
                button.setVisible(false);
            hydr_countVisible = false;
        }
        if (hydrVisible) {
            for (HydrButton button : hydr)
                button.setVisible(false);
            hydrVisible = false;
        }
        if (hydr_outVisible) {
            if (hydr_out_index != 0) {
                for (int i = 0; i < hydr_out_index; ++i)
                    hydr_out[i].setVisible(false);
            }
            hydr_outVisible = false;
        }
        if (ext_countVisible) {
            for (ExtCountButton button : ext_count)
                button.setVisible(false);
            ext_countVisible = false;
        }
        if (hydr_extVisible) {
            if (hydr_ext_index != 0) {
                for (int i = 0; i < hydr_ext_index; ++i)
                    hydr_ext[i].setVisible(false);
            }
            hydr_extVisible = false;
        }
        if (hydr_ext_bVisible) {
            if (hydr_ext_b_index != 0) {
                for (int i = 0; i < hydr_ext_b_index; ++i)
                    hydr_ext_b[i].setVisible(false);
            }
            hydr_ext_bVisible = false;
        }
        if (hydr_ext_b_fromVisible) {
            if (hydr_ext_b_from_index != 0) {
                for (int i = 0; i < hydr_ext_b_from_index; ++i)
                    hydr_ext_b_from[i].setVisible(false);
            }
            hydr_ext_b_fromVisible = false;
        }
    }

    public void showEvacuation() {
        if (!evacVisible) {
            for (EvacuateButton button : evac) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            evacVisible = true;
        }
        if (sensVisible) {
            for (FireSensorButton sensor : sens) {
                if (sensor.isSelected())
                    continue;
                sensor.setVisible(false);
            }
            sensVisible = false;
        }
        if (fireVisible) {
            for (FireButton button : fire_btn) {
                if (button.isSelected())
                    continue;
                button.setVisible(false);
            }
            fireVisible = false;
        }
        if (ventVisible) {
            for (AirIsolButton button : ventil)
                button.setVisible(false);
            ventVisible = false;
        }
        if (drVisible) {
            for (DoorIsolButton button : dr)
                button.setVisible(false);
            drVisible = false;
        }
        if (explVisible) {
            for (ExplosiveButton button : expl)
                button.setVisible(false);
            explVisible = false;
        }
        if (isolVisible) {
            for (IsolationButton button : isol)
                button.setVisible(false);
            isolVisible = false;
        }
        if (isol_mechVisible) {
            for (IsolationButton button : isol_mech)
                button.setVisible(false);
            isol_mechVisible = false;
        }
        if (hydr_countVisible) {
            for (HydrCountButton button : hydr_count)
                button.setVisible(false);
            hydr_countVisible = false;
        }
        if (hydrVisible) {
            for (HydrButton button : hydr)
                button.setVisible(false);
            hydrVisible = false;
        }
        if (hydr_outVisible) {
            if (hydr_out_index != 0) {
                for (int i = 0; i < hydr_out_index; ++i)
                    hydr_out[i].setVisible(false);
            }
            hydr_outVisible = false;
        }
        if (ext_countVisible) {
            for (ExtCountButton button : ext_count)
                button.setVisible(false);
            ext_countVisible = false;
        }
        if (hydr_extVisible) {
            if (hydr_ext_index != 0) {
                for (int i = 0; i < hydr_ext_index; ++i)
                    hydr_ext[i].setVisible(false);
            }
            hydr_extVisible = false;
        }
        if (hydr_ext_bVisible) {
            if (hydr_ext_b_index != 0) {
                for (int i = 0; i < hydr_ext_b_index; ++i)
                    hydr_ext_b[i].setVisible(false);
            }
            hydr_ext_bVisible = false;
        }
        if (hydr_ext_b_fromVisible) {
            if (hydr_ext_b_from_index != 0) {
                for (int i = 0; i < hydr_ext_b_from_index; ++i)
                    hydr_ext_b_from[i].setVisible(false);
            }
            hydr_ext_b_fromVisible = false;
        }
    }

    public void showGermetisation() {
        if (!ventVisible) {
            for (AirIsolButton button : ventil) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            ventVisible = true;
        }
        if (!drVisible) {
            for (DoorIsolButton button : dr) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            drVisible = true;
        }
        if (sensVisible) {
            for (FireSensorButton sensor : sens) {
                if (sensor.isSelected())
                    continue;
                sensor.setVisible(false);
            }
            sensVisible = false;
        }
        if (fireVisible) {
            for (FireButton button : fire_btn) {
                if (button.isSelected())
                    continue;
                button.setVisible(false);
            }
            fireVisible = false;
        }
        if (evacVisible) {
            for (EvacuateButton button : evac)
                button.setVisible(false);
            evacVisible = false;
        }
        if (explVisible) {
            for (ExplosiveButton button : expl)
                button.setVisible(false);
            explVisible = false;
        }
        if (isolVisible) {
            for (IsolationButton button : isol)
                button.setVisible(false);
            isolVisible = false;
        }
        if (isol_mechVisible) {
            for (IsolationButton button : isol_mech)
                button.setVisible(false);
            isol_mechVisible = false;
        }
        if (hydr_countVisible) {
            for (HydrCountButton button : hydr_count)
                button.setVisible(false);
            hydr_countVisible = false;
        }
        if (hydrVisible) {
            for (HydrButton button : hydr)
                button.setVisible(false);
            hydrVisible = false;
        }
        if (hydr_outVisible) {
            if (hydr_out_index != 0) {
                for (int i = 0; i < hydr_out_index; ++i)
                    hydr_out[i].setVisible(false);
            }
            hydr_outVisible = false;
        }
        if (ext_countVisible) {
            for (ExtCountButton button : ext_count)
                button.setVisible(false);
            ext_countVisible = false;
        }
        if (hydr_extVisible) {
            if (hydr_ext_index != 0) {
                for (int i = 0; i < hydr_ext_index; ++i)
                    hydr_ext[i].setVisible(false);
            }
            hydr_extVisible = false;
        }
        if (hydr_ext_bVisible) {
            if (hydr_ext_b_index != 0) {
                for (int i = 0; i < hydr_ext_b_index; ++i)
                    hydr_ext_b[i].setVisible(false);
            }
            hydr_ext_bVisible = false;
        }
        if (hydr_ext_b_fromVisible) {
            if (hydr_ext_b_from_index != 0) {
                for (int i = 0; i < hydr_ext_b_from_index; ++i)
                    hydr_ext_b_from[i].setVisible(false);
            }
            hydr_ext_b_fromVisible = false;
        }
    }

    public void showPrevention() {
        if (!isolVisible) {
            for (IsolationButton button : isol) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            isolVisible = true;
        }
        if (!isol_mechVisible) {
            for (IsolationButton button : isol_mech) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            isol_mechVisible = true;
        }
        if (!explVisible) {
            for (ExplosiveButton button : expl) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            explVisible = true;
        }
        if (sensVisible) {
            for (FireSensorButton sensor : sens) {
                if (sensor.isSelected())
                    continue;
                sensor.setVisible(false);
            }
            sensVisible = false;
        }
        if (fireVisible) {
            for (FireButton button : fire_btn) {
                if (button.isSelected())
                    continue;
                button.setVisible(false);
            }
            fireVisible = false;
        }
        if (evacVisible) {
            for (EvacuateButton button : evac)
                button.setVisible(false);
            evacVisible = false;
        }
        if (ventVisible) {
            for (AirIsolButton button : ventil)
                button.setVisible(false);
            ventVisible = false;
        }
        if (drVisible) {
            for (DoorIsolButton button : dr)
                button.setVisible(false);
            drVisible = false;
        }
        if (hydr_countVisible) {
            for (HydrCountButton button : hydr_count)
                button.setVisible(false);
            hydr_countVisible = false;
        }
        if (hydrVisible) {
            for (HydrButton button : hydr)
                button.setVisible(false);
            hydrVisible = false;
        }
        if (hydr_outVisible) {
            if (hydr_out_index != 0) {
                for (int i = 0; i < hydr_out_index; ++i)
                    hydr_out[i].setVisible(false);
            }
            hydr_outVisible = false;
        }
        if (ext_countVisible) {
            for (ExtCountButton button : ext_count)
                button.setVisible(false);
            ext_countVisible = false;
        }
        if (hydr_extVisible) {
            if (hydr_ext_index != 0) {
                for (int i = 0; i < hydr_ext_index; ++i)
                    hydr_ext[i].setVisible(false);
            }
            hydr_extVisible = false;
        }
        if (hydr_ext_bVisible) {
            if (hydr_ext_b_index != 0) {
                for (int i = 0; i < hydr_ext_b_index; ++i)
                    hydr_ext_b[i].setVisible(false);
            }
            hydr_ext_bVisible = false;
        }
        if (hydr_ext_b_fromVisible) {
            if (hydr_ext_b_from_index != 0) {
                for (int i = 0; i < hydr_ext_b_from_index; ++i)
                    hydr_ext_b_from[i].setVisible(false);
            }
            hydr_ext_b_fromVisible = false;
        }
    }

    public void showForcesArrangement() {
        if (!hydr_countVisible) {
            for (HydrCountButton button : hydr_count) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            hydr_countVisible = true;
        }
        if (!hydrVisible) {
            for (HydrButton button : hydr) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            hydrVisible = true;
        }
        if (!hydr_outVisible) {
            for (int i = 0; i < hydr_out_index; ++i) {
                if (!hydr_out[i].isEnabled())
                    continue;
                hydr_out[i].setVisible(true);
            }
            hydr_outVisible = true;
        }
        if (sensVisible) {
            for (FireSensorButton sensor : sens) {
                if (sensor.isSelected())
                    continue;
                sensor.setVisible(false);
            }
            sensVisible = false;
        }
        if (fireVisible) {
            for (FireButton button : fire_btn) {
                if (button.isSelected())
                    continue;
                button.setVisible(false);
            }
            fireVisible = false;
        }
        if (evacVisible) {
            for (EvacuateButton button : evac)
                button.setVisible(false);
            evacVisible = false;
        }
        if (ventVisible) {
            for (AirIsolButton button : ventil)
                button.setVisible(false);
            ventVisible = false;
        }
        if (drVisible) {
            for (DoorIsolButton button : dr)
                button.setVisible(false);
            drVisible = false;
        }
        if (explVisible) {
            for (ExplosiveButton button : expl)
                button.setVisible(false);
            explVisible = false;
        }
        if (isolVisible) {
            for (IsolationButton button : isol)
                button.setVisible(false);
            isolVisible = false;
        }
        if (isol_mechVisible) {
            for (IsolationButton button : isol_mech)
                button.setVisible(false);
            isol_mechVisible = false;
        }
        if (ext_countVisible) {
            for (ExtCountButton button : ext_count)
                button.setVisible(false);
            ext_countVisible = false;
        }
        if (hydr_extVisible) {
            if (hydr_ext_index != 0) {
                for (int i = 0; i < hydr_ext_index; ++i)
                    hydr_ext[i].setVisible(false);
            }
            hydr_extVisible = false;
        }
        if (hydr_ext_bVisible) {
            if (hydr_ext_b_index != 0) {
                for (int i = 0; i < hydr_ext_b_index; ++i)
                    hydr_ext_b[i].setVisible(false);
            }
            hydr_ext_bVisible = false;
        }
        if (hydr_ext_b_fromVisible) {
            if (hydr_ext_b_from_index != 0) {
                for (int i = 0; i < hydr_ext_b_from_index; ++i)
                    hydr_ext_b_from[i].setVisible(false);
            }
            hydr_ext_b_fromVisible = false;
        }
    }

    public void showFireExtinguishing() {
        if (!ext_countVisible) {
            for (ExtCountButton button : ext_count) {
                if (!button.isEnabled())
                    continue;
                button.setVisible(true);
            }
            ext_countVisible = true;
        }
        if (!hydr_extVisible) {
            for (int i = 0; i < hydr_ext_index; ++i) {
                if (!hydr_ext[i].isEnabled())
                    continue;
                hydr_ext[i].setVisible(true);
            }
            hydr_extVisible = true;
        }
        if (!hydr_ext_bVisible) {
            for (int i = 0; i < hydr_ext_b_index; ++i) {
                if (!hydr_ext_b[i].isEnabled())
                    continue;
                hydr_ext_b[i].setVisible(true);
            }
            hydr_ext_bVisible = true;
        }
        if (!hydr_ext_b_fromVisible) {
            for (int i = 0; i < hydr_ext_b_from_index; ++i) {
                if (!hydr_ext_b_from[i].isEnabled())
                    continue;
                hydr_ext_b_from[i].setVisible(true);
            }
            hydr_ext_b_fromVisible = true;
        }
        if (sensVisible) {
            for (FireSensorButton sensor : sens) {
                if (sensor.isSelected())
                    continue;
                sensor.setVisible(false);
            }
            sensVisible = false;
        }
        if (fireVisible) {
            for (FireButton button : fire_btn) {
                if (button.isSelected())
                    continue;
                button.setVisible(false);
            }
            fireVisible = false;
        }
        if (evacVisible) {
            for (EvacuateButton button : evac)
                button.setVisible(false);
            evacVisible = false;
        }
        if (ventVisible) {
            for (AirIsolButton button : ventil)
                button.setVisible(false);
            ventVisible = false;
        }
        if (drVisible) {
            for (DoorIsolButton button : dr)
                button.setVisible(false);
            drVisible = false;
        }
        if (hydr_countVisible) {
            for (HydrCountButton button : hydr_count)
                button.setVisible(false);
            }
            hydr_countVisible = false;
        if (hydrVisible) {
            for (HydrButton button : hydr)
                button.setVisible(false);
            hydrVisible = false;
        }
        if (hydr_outVisible) {
            if (hydr_out_index != 0) {
                for (int i = 0; i < hydr_out_index; ++i)
                    hydr_out[i].setVisible(false);
            }
            hydr_outVisible = false;
        }
        if (explVisible) {
            for (ExplosiveButton button : expl)
                button.setVisible(false);
            explVisible = false;
        }
        if (isolVisible) {
            for (IsolationButton button : isol)
                button.setVisible(false);
            isolVisible = false;
        }
        if (isol_mechVisible) {
            for (IsolationButton button : isol_mech)
                button.setVisible(false);
            isol_mechVisible = false;
        }
    }

    public static void main(final String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception e) {
            System.err.println("Cannot set UTF-8 for log output: " + e.getMessage());
        }

        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run() {
                Locale locale = hasArgument() ? new Locale(args[0]) : null;
                Localization.init(locale);
                new Main();
            }

            private boolean hasArgument() {
                return args.length > 0 && !args[0].isEmpty();
            }
        });
    }

    //================================================================
    // TABLE MODELS (для FIRE EVENTS TABLE и RECOMMENDED ACTIONS TABLE)
    // Внутренние классы моделей таблиц. Связаны с группами FIRE EVENTS TABLE и RECOMMENDED ACTIONS TABLE.
    //================================================================
    public static class ActionsTableModel extends AbstractTableModel {
        Vector<Vector<Object>> row = new Vector<Vector<Object>>();
        Vector<String> cols = new Vector<String>();
        Vector<Vector<String>> headers = new Vector<Vector<String>>();

        public ActionsTableModel() {
            cols.add("");
            cols.add("");
            cols.add("");
            cols.add("");
            headers.add(cols);
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0: return Localization.get("action.column.phase");
                case 1: return Localization.get("label.number");
                case 2: return Localization.get("action.column.room");
                case 3: return Localization.get("action.column.recommendation");
                default: return null;
            }
        }

        @Override
        public int getRowCount() {
            return row.size();
        }

        @Override
        public int getColumnCount() {
            return cols.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Vector<Object> rowVector = row.get(rowIndex);
            return rowVector.get(columnIndex);
        }

        public void addRow(String phase, int num, String room, String recommendation) {
            Vector<Object> rowData = new Vector<Object>();
            rowData.add(phase);
            rowData.add(num);
            rowData.add(room);
            rowData.add(recommendation);
            row.add(rowData);
        }

        public void removeRow() {
            row.removeAllElements();
        }
    }

    public static class EventsTableModel extends AbstractTableModel {
        Vector<Vector<Object>> row = new Vector<Vector<Object>>();
        Vector<String> cols = new Vector<String>();
        Vector<Vector<String>> headers = new Vector<Vector<String>>();

        public EventsTableModel() {
            cols.add("");
            cols.add("");
            cols.add("");
            cols.add("");
            cols.add("");
            cols.add("");
            headers.add(cols);
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0: return Localization.get("label.number");
                case 1: return Localization.get("event.column.datetime");
                case 2: return Localization.get("event.column.room");
                case 3: return Localization.get("event.column.deck");
                case 4: return Localization.get("event.column.compartment");
                case 5: return Localization.get("event.column.type");
                default: return null;
            }
        }

        @Override
        public int getRowCount() {
            return row.size();
        }

        @Override
        public int getColumnCount() {
            return cols.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Vector<Object> rowVector = row.get(rowIndex);
            return rowVector.get(columnIndex);
        }

        public void addRow(int num, String dateTime, String room, String deck, String compartment, String event) {
            Vector<Object> rowData = new Vector<Object>();
            rowData.add(num);
            rowData.add(dateTime);
            rowData.add(room);
            rowData.add(deck);
            rowData.add(compartment);
            rowData.add(event);
            row.add(rowData);
        }
    }

    //================================================================
    // BUTTONS (все custom *Button классы)
    // Внутренние классы для интерактивных элементов на карте (гидранты, двери, эвакуация, изоляция и т.д.).
    // Каждый переопределяет paintComponent для отрисовки символов. Связаны с группой MAP PANEL.
    //================================================================
    public static class HydrExtBorderFromButton extends JToggleButton {
        String title;
        int size;

        public HydrExtBorderFromButton(String title) {
            this.title = title;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(orange);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_orange);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            } else {
                g.setColor(green);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_green);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            }
            Font font = g.getFont();
            g.setColor(Color.BLACK);
            g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
            if (size == 1) {
                g.drawString(title, 5, 11);
            } else {
                g.drawString(String.format("hd_%s", title.substring(5)), 4, 11);
            }
            g.setFont(font);
            if (size == 1) {
                g.drawLine(46, 6, 46, 10);
                g.drawLine(46, 8, 60, 8);
                g.drawLine(54, 6, 60, 8);
                g.drawLine(54, 10, 60, 8);
                g.drawOval(61, 7, 2, 2);
            } else {
                g.drawLine(13, 17, 13, 21);
                g.drawLine(13, 19, 27, 19);
                g.drawLine(21, 17, 27, 19);
                g.drawLine(21, 21, 27, 19);
                g.drawOval(28, 18, 2, 2);
            }
        }
    }

    public static class HydrExtBorderToButton extends JToggleButton {
        String title;
        int size;

        public HydrExtBorderToButton(String title) {
            this.title = title;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(orange);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_orange);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            } else {
                g.setColor(green);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_green);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            }
            Font font = g.getFont();
            g.setColor(Color.BLACK);
            g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
            g.drawString(title, 27, 11);
            g.setFont(font);
            g.drawLine(4, 6, 4, 10);
            g.drawLine(4, 8, 18, 8);
            g.drawLine(12, 6, 18, 8);
            g.drawLine(12, 10, 18, 8);
            g.drawOval(19, 7, 2, 2);
            g.fillRect(0, 0, 2, getSize().height);
        }
    }

    public static class HydrExtButton extends JToggleButton {
        String title;
        int size;

        public HydrExtButton(String title) {
            this.title = title;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(red);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_red);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            } else {
                g.setColor(green);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_green);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            }
            Font font = g.getFont();
            g.setColor(Color.BLACK);
            g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
            g.drawString(title, 27, 11);
            g.setFont(font);
            g.drawLine(4, 6, 4, 10);
            g.drawLine(4, 8, 18, 8);
            g.drawLine(12, 6, 18, 8);
            g.drawLine(12, 10, 18, 8);
            g.drawOval(19, 7, 2, 2);
            g.fillRect(0, 0, 2, getSize().height);
        }
    }

    public static class ExtCountButton extends JLabel {
        String from;
        String number;
        String size;

        public ExtCountButton(String from, String number, String size) {
            this.from = from;
            this.number = number;
            this.size = size;
        }

        public void setLabels(String from, String number) {
            this.from = from.substring(1, 2).toUpperCase();
            this.number = number.substring(0, 1);
        }

        public String getLabelSize() {
            return size;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(green);
            g.fillRect(0, 0, getSize().width, getSize().height);
            g.setColor(dark_green);
            g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            Font font = g.getFont();
            g.setColor(Color.BLACK);
            g.setFont(new Font(font.getFontName(), Font.BOLD, font.getSize()));
            if (size.equals("full")) {
                g.drawString(number, 5, 14);
                g.drawString(from, 33, 14);
                g.drawLine(14, 9, 28, 9);
                g.drawLine(14, 9, 20, 7);
                g.drawLine(14, 9, 20, 11);
            } else {
                g.drawString(number, 5, 14);
                g.setFont(new Font(font.getFontName(), Font.PLAIN, font.getSize() - 2));
                g.drawString(from, 23, 13);
                g.drawLine(14, 9, 20, 9);
                g.drawLine(16, 8, 16, 10);
            }
            g.setFont(font);
        }
    }

    public static class HydrOutButton extends JToggleButton {
        String title;
        int size;

        public HydrOutButton(String title) {
            this.title = title;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(red);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_red);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            } else {
                g.setColor(green);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_green);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            }
            Font font = g.getFont();
            g.setColor(Color.BLACK);
            g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
            if (size == 1) {
                g.drawString(title, 5, 11);
            } else {
                g.drawString(String.format("hd_%s", title.substring(5)), 4, 11);
            }
            g.setFont(font);
            if (size == 1) {
                g.drawOval(50, 4, 6, 6);
                g.drawLine(50, 4, 56, 10);
                g.drawLine(56, 4, 50, 10);
                g.drawOval(58, 4, 6, 6);
                g.drawLine(58, 4, 64, 10);
                g.drawLine(64, 4, 58, 10);
            } else {
                g.drawOval(16, 16, 6, 6);
                g.drawLine(16, 16, 22, 22);
                g.drawLine(22, 16, 16, 22);
                g.drawOval(24, 16, 6, 6);
                g.drawLine(24, 16, 30, 22);
                g.drawLine(30, 16, 24, 22);
            }
            g.fillRect(0, 0, 2, getSize().height);
        }
    }

    public static class HydrButton extends JLabel {
        int free;
        int size;
        String title;

        public HydrButton(int free, int size) {
            this.free = free;
            this.size = size;
        }

        public void setNumbers(String free, String title) {
            this.free = Integer.parseInt(free);
            this.title = title;
        }

        public int getLabelSize() {
            return size;
        }

        @Override
        public void paintComponent(Graphics g) {
            int x;
            int i;
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getSize().width, getSize().height);
            g.setColor(mid_dark_red);
            g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            for (i = 0; i < free; ++i) {
                x = i * 14;
                g.fillOval(x + 2, 7, 13, 13);
                g.fillRect(x + 4, 2, 9, 3);
                g.drawLine(x + 3, 3, x + 13, 3);
                g.fillRect(x + 7, 4, 3, 3);
            }
            if (size != free) {
                g.setColor(blue);
                for (i = 0; i < size - free; ++i) {
                    x = (i + free) * 14;
                    g.fillOval(x + 2, 7, 13, 13);
                    g.fillRect(x + 4, 2, 9, 3);
                    g.drawLine(x + 3, 3, x + 13, 3);
                    g.fillRect(x + 7, 4, 3, 3);
                }
            }
            Font font = g.getFont();
            g.setColor(mid_dark_red);
            g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 4));
            if (size == 1) {
                g.drawLine(3, 21, 3, 26);
                g.drawLine(4, 23, 4, 23);
                g.drawLine(5, 24, 5, 26);
                g.drawLine(7, 27, 9, 27);
                g.drawString(title.substring(5), 10, 27);
            } else {
                g.drawLine(3, 21, 3, 26);
                g.drawLine(4, 23, 4, 23);
                g.drawLine(5, 24, 5, 26);
                g.drawLine(7, 23, 7, 25);
                g.drawLine(8, 26, 8, 26);
                g.drawLine(9, 23, 9, 27);
                g.drawLine(8, 28, 8, 28);
                g.drawLine(11, 24, 11, 25);
                g.drawLine(12, 23, 12, 23);
                g.drawLine(12, 26, 12, 26);
                g.drawLine(13, 21, 13, 26);
                g.drawLine(15, 23, 15, 26);
                g.drawLine(16, 24, 16, 24);
                g.drawLine(17, 23, 17, 23);
                g.drawLine(17, 27, 18, 27);
                g.drawString(title.substring(5), 20, 27);
            }
            g.setFont(font);
        }
    }

    public static class HydrCountButton extends JLabel {
        String here;
        String need;
        String size;

        public HydrCountButton(String here, String need, String size) {
            this.here = here;
            this.need = need;
            this.size = size;
        }

        public void setNumbers(String here, String need) {
            this.here = here.substring(1, 2);
            String needDigit = need.substring(1, 2);
            int total = Integer.parseInt(this.here) + Integer.parseInt(needDigit);
            this.need = String.valueOf(total);
        }

        public String getLabelSize() {
            return size;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!here.equals(need)) {
                g.setColor(red);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_red);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            } else {
                g.setColor(green);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_green);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            }
            Font font = g.getFont();
            g.setColor(Color.BLACK);
            g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
            if (size.equals("full")) {
                g.drawString(String.format(Localization.get("label.hydr.count.full"), here, need), 4, 13);
            } else {
                g.drawString(String.format(Localization.get("label.hydr.count.short"), here, need), 3, 13);
            }
            g.setFont(font);
        }
    }

    public static class IsolationButton extends JToggleButton {
        String letter;

        public IsolationButton(String letter) {
            this.letter = letter;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(red);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_red);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            } else {
                g.setColor(green);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_green);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            }
            if (letter.equals("O")) {
                g.setColor(brown);
                g.fillRect(3, 5, 13, 11);
                if (!super.isSelected()) {
                    g.setColor(red);
                } else {
                    g.setColor(green);
                }
                g.fillRect(4, 6, 11, 9);
            }
            if (letter.equals("M")) {
                g.setColor(Color.BLACK);
                g.fillRect(8, 3, 3, 13);
                g.fillRect(3, 8, 13, 3);
                g.drawLine(5, 5, 13, 13);
                g.drawLine(5, 4, 14, 13);
                g.drawLine(4, 5, 13, 14);
                g.drawLine(13, 5, 5, 13);
                g.drawLine(13, 4, 4, 13);
                g.drawLine(14, 5, 5, 14);
                g.fillOval(4, 4, 11, 11);
                if (!super.isSelected()) {
                    g.setColor(red);
                } else {
                    g.setColor(green);
                }
                g.fillOval(5, 5, 9, 9);
                g.setColor(Color.BLACK);
                g.fillOval(8, 8, 3, 3);
                g.drawLine(6, 6, 6, 6);
                g.drawLine(6, 12, 6, 12);
            }
            if (letter.equals("C")) {
                g.setColor(Color.BLACK);
                g.drawLine(2, 8, 16, 8);
                g.drawLine(2, 6, 8, 5);
                g.drawLine(10, 5, 16, 6);
                g.drawLine(8, 6, 10, 6);
                g.drawLine(2, 7, 2, 7);
                g.drawLine(16, 7, 16, 7);
                g.fillRect(6, 8, 7, 6);
                if (!super.isSelected()) {
                    g.setColor(red);
                } else {
                    g.setColor(green);
                }
                g.fillRect(7, 7, 5, 6);
            }
        }
    }

    public static class FireButton extends JToggleButton {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(light_pastele_grey);
            } else {
                g.setColor(red);
            }
            g.fillRect(0, 0, getSize().width - 1, getSize().height - 1);
            if (!super.isSelected()) {
                g.setColor(dark_orange);
            } else {
                g.setColor(dark_red);
            }
            g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            if (!super.isSelected()) {
                g.setColor(dark_red);
            } else {
                g.setColor(Color.BLACK);
            }
            g.drawLine(3, 8, 3, 8);
            g.drawLine(2, 5, 2, 7);
            g.drawLine(3, 4, 5, 2);
            g.drawLine(5, 3, 5, 4);
            g.drawLine(6, 5, 6, 6);
            g.drawLine(7, 3, 7, 4);
            g.drawLine(8, 5, 8, 7);
            g.drawLine(7, 8, 7, 8);
            if (!super.isSelected()) {
                g.setColor(pastele_orange);
            } else {
                g.setColor(mid_dark_red);
            }
            g.drawLine(3, 5, 3, 7);
            g.drawLine(4, 4, 4, 8);
            g.drawLine(5, 5, 5, 8);
            g.drawLine(6, 7, 6, 8);
            g.drawLine(7, 5, 7, 7);
        }
    }

    public static class FireSensorButton extends JToggleButton {
        String letter;

        public FireSensorButton(String letter) {
            this.letter = letter;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(light_pastele_grey);
            } else {
                g.setColor(red);
            }
            g.fillRect(0, 0, getSize().width - 1, getSize().height - 1);
            if (!super.isSelected()) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(dark_red);
            }
            g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            if (letter.equals("RI")) {
                g.setColor(Color.BLACK);
                g.drawLine(9, 9, 9, 15);
                g.drawArc(5, 0, 8, 8, 180, 190);
            } else {
                Font font = g.getFont();
                g.setColor(Color.BLACK);
                g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
                if (letter.equals("K")) {
                    g.drawString(letter, 6, 9);
                } else {
                    g.drawString(letter, 7, 9);
                }
                g.setFont(font);
                g.setColor(Color.BLACK);
                g.fillOval(6, 10, 7, 7);
                g.drawLine(2, 13, 16, 13);
            }
        }
    }

    public static class ExplosiveButton extends JToggleButton {
        String letter;

        public ExplosiveButton(String letter) {
            this.letter = letter;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(red);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_red);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            } else {
                g.setColor(green);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_green);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            }
            if (letter.equals("A") | letter.equals("R")) {
                if (letter.equals("A")) {
                    g.setColor(sky_blue);
                }
                if (letter.equals("R")) {
                    g.setColor(co_gray);
                }
                g.fillRect(4, 6, 11, 10);
                g.drawLine(5, 3, 5, 6);
                g.drawLine(9, 3, 9, 6);
                g.drawLine(13, 3, 13, 6);
                if (letter.equals("A")) {
                    g.setColor(Color.WHITE);
                }
                if (letter.equals("R")) {
                    g.setColor(Color.YELLOW);
                }
                g.drawLine(4, 8, 14, 8);
                g.setColor(Color.BLACK);
                g.drawRoundRect(3, 5, 4, 11, 2, 2);
                g.drawRoundRect(7, 5, 4, 11, 2, 2);
                g.drawRoundRect(11, 5, 4, 11, 2, 2);
                g.drawLine(3, 16, 15, 16);
                g.drawRoundRect(4, 2, 2, 3, 2, 2);
                g.drawRoundRect(8, 2, 2, 3, 2, 2);
                g.drawRoundRect(12, 2, 2, 3, 2, 2);
            } else {
                g.setColor(brown);
                g.fillRect(3, 5, 13, 11);
                if (!super.isSelected()) {
                    g.setColor(red);
                } else {
                    g.setColor(green);
                }
                g.fillRect(4, 6, 11, 9);
                g.setColor(brown);
                g.drawLine(15, 5, 3, 15);
            }
        }
    }

    public static class EvacuateButton extends JToggleButton {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(red);
            } else {
                g.setColor(green);
            }
            g.fillRect(0, 0, getSize().width, getSize().height);
            if (!super.isSelected()) {
                g.setColor(dark_red);
            } else {
                g.setColor(dark_green);
            }
            g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
            g.setColor(Color.BLACK);
            g.fillRect(7, 3, 9, 13);
            if (!super.isSelected()) {
                g.setColor(red);
            } else {
                g.setColor(green);
            }
            g.fillRect(9, 5, 5, 9);
            g.fillRect(7, 7, 2, 5);
            g.setColor(Color.BLACK);
            g.drawLine(3, 9, 10, 9);
            g.drawLine(4, 8, 4, 10);
            g.drawLine(5, 7, 5, 11);
        }
    }

    public static class DoorIsolButton extends JToggleButton {
        String door_type;
        String door_orientation;
        String door_side;
        String door_direction;
        String door_keep_open;

        public DoorIsolButton(String door_type, String door_orientation, String door_side, String door_direction, String door_keep_open) {
            this.door_type = door_type;
            this.door_orientation = door_orientation;
            this.door_side = door_side;
            this.door_direction = door_direction;
            this.door_keep_open = door_keep_open;
        }

        public void setKeepOpen(String door_keep_open) {
            this.door_keep_open = door_keep_open;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                if (door_keep_open.equals("no")) {
                    g.setColor(red);
                } else {
                    g.setColor(orange);
                }
            } else {
                g.setColor(green);
            }
            g.fillRect(0, 0, getSize().width, getSize().height);
            g.setColor(Color.BLACK);
            if (door_type.equals("custom")) {
                if (door_orientation.equals("vertical")) {
                    if (door_side.equals("right")) {
                        if (door_direction.equals("top")) {
                            if (!super.isSelected()) {
                                g.fillOval(-30, 1, 58, 58);
                                if (door_keep_open.equals("no")) {
                                    g.setColor(red);
                                } else {
                                    g.setColor(orange);
                                }
                                g.fillOval(-29, 2, 56, 56);
                                g.fillRect(16, 0, getSize().width, getSize().height);
                                g.setColor(Color.BLACK);
                                g.drawLine(0, 30, 11, 4);
                            } else {
                                g.drawLine(2, 2, 3, 2);
                                g.drawLine(3, 2, 3, 27);
                                g.drawLine(2, 27, 3, 27);
                                g.drawLine(4, 4, 8, 4);
                            }
                        } else if (!super.isSelected()) {
                            g.fillOval(-30, -29, 58, 58);
                            if (door_keep_open.equals("no")) {
                                g.setColor(red);
                            } else {
                                g.setColor(orange);
                            }
                            g.fillOval(-29, -28, 56, 56);
                            g.fillRect(16, 0, getSize().width, getSize().height);
                            g.setColor(Color.BLACK);
                            g.drawLine(0, 0, 11, 25);
                        } else {
                            g.drawLine(2, 2, 3, 2);
                            g.drawLine(3, 2, 3, 27);
                            g.drawLine(2, 27, 3, 27);
                            g.drawLine(4, 26, 8, 26);
                        }
                        if (!super.isSelected()) {
                            if (door_keep_open.equals("no")) {
                                g.setColor(dark_red);
                            } else {
                                g.setColor(dark_orange);
                            }
                        } else {
                            g.setColor(dark_green);
                        }
                        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
                        g.setColor(Color.BLACK);
                        g.fillRect(0, 0, 2, getSize().height);
                    } else {
                        if (door_direction.equals("top")) {
                            if (!super.isSelected()) {
                                g.fillOval(-10, 1, 58, 58);
                                if (door_keep_open.equals("no")) {
                                    g.setColor(red);
                                } else {
                                    g.setColor(orange);
                                }
                                g.fillOval(-9, 2, 56, 56);
                                g.fillRect(0, 0, 2, getSize().height);
                                g.setColor(Color.BLACK);
                                g.drawLine(17, 28, 6, 4);
                            } else {
                                g.drawLine(14, 2, 15, 2);
                                g.drawLine(14, 2, 14, 27);
                                g.drawLine(14, 27, 15, 27);
                                g.drawLine(13, 4, 9, 4);
                            }
                        } else if (!super.isSelected()) {
                            g.fillOval(-10, -29, 58, 58);
                            if (door_keep_open.equals("no")) {
                                g.setColor(red);
                            } else {
                                g.setColor(orange);
                            }
                            g.fillOval(-9, -28, 56, 56);
                            g.fillRect(0, 0, 2, getSize().height);
                            g.setColor(Color.BLACK);
                            g.drawLine(17, 0, 6, 25);
                        } else {
                            g.drawLine(14, 2, 15, 2);
                            g.drawLine(14, 2, 14, 27);
                            g.drawLine(14, 27, 15, 27);
                            g.drawLine(13, 26, 9, 26);
                        }
                        if (!super.isSelected()) {
                            if (door_keep_open.equals("no")) {
                                g.setColor(dark_red);
                            } else {
                                g.setColor(dark_orange);
                            }
                        } else {
                            g.setColor(dark_green);
                        }
                        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
                        g.setColor(Color.BLACK);
                        g.fillRect(getSize().width - 2, 0, 2, getSize().height);
                    }
                } else if (door_side.equals("left")) {
                    if (door_direction.equals("top")) {
                        if (!super.isSelected()) {
                            g.fillOval(1, -10, 58, 58);
                            if (door_keep_open.equals("no")) {
                                g.setColor(red);
                            } else {
                                g.setColor(orange);
                            }
                            g.fillOval(2, -9, 56, 56);
                            g.fillRect(0, 0, getSize().width, 2);
                            g.setColor(Color.BLACK);
                            g.drawLine(4, 7, 28, 16);
                        } else {
                            g.drawLine(2, 14, 2, 15);
                            g.drawLine(2, 14, 27, 14);
                            g.drawLine(27, 14, 27, 15);
                            g.drawLine(3, 9, 3, 13);
                        }
                    } else if (!super.isSelected()) {
                        g.fillOval(1, -30, 58, 58);
                        if (door_keep_open.equals("no")) {
                            g.setColor(red);
                        } else {
                            g.setColor(orange);
                        }
                        g.fillOval(2, -29, 56, 56);
                        g.fillRect(0, 16, getSize().width, getSize().height);
                        g.setColor(Color.BLACK);
                        g.drawLine(4, 11, 30, 0);
                    } else {
                        g.drawLine(2, 2, 2, 2);
                        g.drawLine(2, 3, 27, 3);
                        g.drawLine(27, 2, 27, 3);
                        g.drawLine(3, 4, 3, 8);
                    }
                    if (!super.isSelected()) {
                        if (door_keep_open.equals("no")) {
                            g.setColor(dark_red);
                        } else {
                            g.setColor(dark_orange);
                        }
                    } else {
                        g.setColor(dark_green);
                    }
                    g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
                    g.setColor(Color.BLACK);
                    if (door_direction.equals("top")) {
                        g.fillRect(0, getSize().height - 2, getSize().width, 2);
                    } else {
                        g.fillRect(0, 0, getSize().width, 2);
                    }
                }
            } else {
                g.setColor(Color.BLACK);
                if (door_orientation.equals("vertical")) {
                    if (door_direction.equals("top")) {
                        if (!super.isSelected()) {
                            g.fillOval(-8, 2, 58, 58);
                            if (door_keep_open.equals("no")) {
                                g.setColor(red);
                            } else {
                                g.setColor(orange);
                            }
                            g.fillOval(-7, 3, 56, 56);
                            g.fillRect(1, 1, 2, getSize().height - 1);
                            g.setColor(Color.BLACK);
                            g.drawLine(19, 29, 8, 6);
                            g.drawLine(20, 27, 9, 6);
                            g.drawLine(20, 29, 19, 26);
                            g.drawLine(17, 23, 17, 23);
                            g.drawLine(15, 19, 15, 19);
                            g.drawLine(13, 15, 13, 15);
                            g.drawLine(11, 11, 11, 11);
                            g.drawLine(9, 7, 9, 7);
                        } else {
                            g.drawRect(18, 2, 2, 28);
                            g.drawLine(17, 3, 13, 3);
                            g.drawLine(19, 3, 19, 5);
                            g.drawLine(19, 7, 19, 10);
                            g.drawLine(19, 12, 19, 15);
                            g.drawLine(19, 17, 19, 20);
                            g.drawLine(19, 22, 19, 25);
                            g.drawLine(19, 27, 19, 29);
                        }
                    } else if (!super.isSelected()) {
                        g.fillOval(-8, -26, 58, 58);
                        if (door_keep_open.equals("no")) {
                            g.setColor(red);
                        } else {
                            g.setColor(orange);
                        }
                        g.fillOval(-7, -25, 56, 56);
                        g.fillRect(1, 1, 2, getSize().height - 1);
                        g.setColor(Color.BLACK);
                        g.drawLine(19, 4, 8, 27);
                        g.drawLine(20, 5, 9, 28);
                        g.drawLine(20, 4, 19, 6);
                        g.drawLine(17, 10, 17, 10);
                        g.drawLine(15, 14, 15, 14);
                        g.drawLine(13, 18, 13, 18);
                        g.drawLine(11, 22, 11, 22);
                        g.drawLine(9, 26, 9, 26);
                    } else {
                        g.drawRect(18, 3, 2, 28);
                        g.drawLine(17, 30, 13, 30);
                        g.drawLine(19, 4, 19, 5);
                        g.drawLine(19, 7, 19, 10);
                        g.drawLine(19, 12, 19, 15);
                        g.drawLine(19, 17, 19, 20);
                        g.drawLine(19, 22, 19, 25);
                        g.drawLine(19, 27, 19, 30);
                    }
                    if (!super.isSelected()) {
                        if (door_keep_open.equals("no")) {
                            g.setColor(dark_red);
                        } else {
                            g.setColor(dark_orange);
                        }
                    } else {
                        g.setColor(dark_green);
                    }
                    g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
                    g.setColor(Color.BLACK);
                    g.fillRect(getSize().width - 1, 0, 1, getSize().height);
                } else {
                    if (!super.isSelected()) {
                        g.fillOval(-26, -26, 58, 58);
                        if (door_keep_open.equals("no")) {
                            g.setColor(red);
                        } else {
                            g.setColor(orange);
                        }
                        g.fillOval(-25, -25, 56, 56);
                        g.fillRect(1, getSize().height - 3, getSize().width, 2);
                        g.setColor(Color.BLACK);
                        g.drawLine(4, 3, 27, 15);
                        g.drawLine(5, 2, 28, 14);
                        g.drawLine(4, 2, 5, 3);
                        g.drawLine(9, 5, 9, 5);
                        g.drawLine(13, 7, 13, 7);
                        g.drawLine(17, 9, 17, 9);
                        g.drawLine(21, 11, 21, 11);
                        g.drawLine(25, 13, 27, 14);
                    } else {
                        g.drawRect(3, 2, 28, 2);
                        g.drawLine(30, 5, 30, 9);
                        g.drawLine(4, 3, 5, 3);
                        g.drawLine(7, 3, 10, 3);
                        g.drawLine(12, 3, 15, 3);
                        g.drawLine(17, 3, 20, 3);
                        g.drawLine(22, 3, 25, 3);
                        g.drawLine(27, 3, 30, 3);
                    }
                    if (!super.isSelected()) {
                        if (door_keep_open.equals("no")) {
                            g.setColor(dark_red);
                        } else {
                            g.setColor(dark_orange);
                        }
                    } else {
                        g.setColor(dark_green);
                    }
                    g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getSize().width, 2);
                }
            }
        }
    }

    public static class AirIsolButton extends JToggleButton {
        String letter;

        public AirIsolButton(String letter) {
            this.letter = letter;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorderPainted(false);
            if (!super.isSelected()) {
                g.setColor(red);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_red);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
                g.setColor(Color.BLACK);
                g.fillOval(1, 1, 17, 17);
                g.setColor(red);
            } else {
                g.setColor(green);
                g.fillRect(0, 0, getSize().width, getSize().height);
                g.setColor(dark_green);
                g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
                g.setColor(Color.BLACK);
                g.fillOval(1, 1, 17, 17);
                g.setColor(green);
            }
            g.fillOval(2, 2, 15, 15);
            g.fillRect(1, 8, 17, 3);
            g.fillRect(8, 1, 3, 17);
            Font font = g.getFont();
            g.setColor(Color.BLACK);
            g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 2));
            if (letter.equals("T")) {
                g.drawString(letter, 7, 13);
            } else {
                g.drawString(letter, 6, 13);
            }
            g.setFont(font);
        }
    }

    //================================================================
    // MAP OVERLAYS (Paint* классы)
    // Внутренние классы для рисования оверлеев на карте (эвакуация, взрывы, линии огня).
    // Связаны с группой MAP PANEL.
    //================================================================
    class PaintFireLines extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            int n = 12;
            if (indexlines != -1) {
                g.setColor(mid_dark_red);
                for (int i = 0; i <= indexlines; ++i) {
                    for (int j = 0; j < Main.borders[fill_lines[i]][2][0] - 1; ++j)
                        g.fillRoundRect(Math.min(Main.borders[fill_lines[i]][0][j], Main.borders[fill_lines[i]][0][j + 1]) - n / 2, Math.min(Main.borders[fill_lines[i]][1][j], Main.borders[fill_lines[i]][1][j + 1]) - n / 2, Math.abs(Main.borders[fill_lines[i]][0][j + 1] - Main.borders[fill_lines[i]][0][j]) + n, Math.abs(Main.borders[fill_lines[i]][1][j + 1] - Main.borders[fill_lines[i]][1][j]) + n, n / 2, n / 2);
                }
            }
            pntLines.repaint();
        }
    }

    class PaintExpl extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (indexexpl != -1) {
                for (int i = 0; i <= indexexpl; ++i) {
                    if (expl[i].isSelected())
                        continue;
                    g.setColor(dark_red);
                    g.fillOval(explosion_dots[fill_expl[i]][0], explosion_dots[fill_expl[i]][1], 29, 29);
                    g.setColor(red);
                    g.fillOval(explosion_dots[fill_expl[i]][0] + 1, explosion_dots[fill_expl[i]][1] + 1, 27, 27);
                }
            }
            pntExpl.repaint();
        }
    }

    class PaintEvac extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (index != -1) {
                int n;
                g.setColor(pastele_red);
                for (n = 0; n <= index; ++n)
                    g.fillPolygon(Main.locations[fill_fire[n]][0], Main.locations[fill_fire[n]][1], Main.locations[fill_fire[n]][2][0]);
                g.setColor(pastele_orange);
                for (n = 0; n <= indexthreat; ++n)
                    g.fillPolygon(Main.locations[fill_threat[n]][0], Main.locations[fill_threat[n]][1], Main.locations[fill_threat[n]][2][0]);
                g.setColor(pastele_grey);
                for (n = 0; n <= indexevac; ++n)
                    g.fillPolygon(Main.locations[fill_evac[n]][0], Main.locations[fill_evac[n]][1], Main.locations[fill_evac[n]][2][0]);
            }
            pntEvac.repaint();
        }
    }
}

