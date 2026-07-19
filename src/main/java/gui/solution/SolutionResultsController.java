package gui.solution;

import gui.Localization;
import clips.ClipsReadOnlyService;
import domain.Explanation;
import domain.Extinguisher;
import domain.types.ExtinguisherType;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import gui.solution.SolutionPhaseTree.PhaseChangeListener;
import util.VisibleForTesting;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class SolutionResultsController implements PhaseChangeListener {
    private static final DateTimeFormatter EVENTS_TABLE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String TITLE_EVACUATION = Localization.get("phase.evacuation");
    private static final String TITLE_SEALING = Localization.get("phase.sealing");
    private static final String TITLE_PREVENTION_OF_FLAMMABLE = Localization.get("phase.prevention.flammable");
    private static final String TITLE_MACHINERY_DAMAGE = Localization.get("phase.prevention.machinery.damage");
    private static final String TITLE_PREVENTION_OF_EXPLOSION = Localization.get("phase.prevention.explosion");
    private static final String TITLE_LOCALIZATION = Localization.get("phase.localization");
    private static final String TITLE_FIREFIGHTING = Localization.get("phase.firefighting");
    private static final String TITLE_IMMEDIATE_MEASURES = Localization.get("phase.immediate");

    private static final String EVENT_VALUE_DECK = Localization.get("label.deck.upper");
    private static final String EVENT_VALUE_COMPARTMENT = Localization.get("label.compartment.third");
    private static final String EVENT_VALUE_TYPE = Localization.get("event.fire");

    private final ClipsReadOnlyService clips;
    private final TopologyModel topology;

    private final EventsTableModel eventsTableModel = new EventsTableModel();
    private final JTable eventsTable;

    private final ActionsTableModel actionsTableModel = new ActionsTableModel();
    private final JTable actionsTable;

    public SolutionResultsController(ClipsReadOnlyService clips, TopologyModel topology) {
        this.clips = clips;
        this.topology = topology;

        eventsTable = createEventsTable(eventsTableModel);
        actionsTable = createActionsTable(actionsTableModel);

        actionsTable.addMouseListener(new ActionsTableMouseAdapter(actionsTable, new JPopupMenu()));
    }

    public JTable getEventsTable() {
        return eventsTable;
    }

    public JTable getActionsTable() {
        return actionsTable;
    }

    public void updateEvents(Location location) {
        String timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(EVENTS_TABLE_TIME_FORMAT);
        String locationCode = location.getCode().toUpperCase();
        eventsTableModel.addRow(eventsTableModel.getRowCount() + 1, timestamp, locationCode, EVENT_VALUE_DECK, EVENT_VALUE_COMPARTMENT, EVENT_VALUE_TYPE);
        eventsTableModel.fireTableDataChanged();
    }

    @Override
    public void onPhaseChanged(SolutionTreeSection newPhase) {
        clearActionsTable();
        switch (newPhase) {
            case ROOT -> {
                // should show all the existing events?
            }
            case PRIORITY_MEASURES -> {
                addExtinguisherRows(clips.getFireLocations());
                addEvacuationRows(clips.collectEvacuationLocations("to-evacuate"));
                addSealingVentilationRows(clips.collectSealingLocations("to-off"));
                addSealingDoorCloseRows(clips.collectSealingDoors("to-close"), false);
            }
            case EVACUATION -> addEvacuationRows(clips.collectEvacuationLocations("to-evacuate"));
            case SEALING -> {
                addSealingVentilationRows(clips.collectSealingLocations("to-off"));
                addSealingDoorCloseRows(clips.collectSealingDoors("to-close"), true);
                addSealingDoorKeepOpenRows(clips.collectSealingDoors("keep-open"));
            }
            case PREVENTION -> {
                addPreventionFlammableRows(clips.collectActionPhase("isolation"));
                addPreventionMachineryDamageRows(clips.collectMachineryDamageLocations("stop"));
                addPreventionExplosionRows(clips.collectActionPhase("explosion"));
            }
            case LOCALIZATION -> {
                Set<Location> flammableRooms = clips.collectActionPhase("isolation");
                Set<Location> machineryDamageRooms = clips.collectMachineryDamageLocations("stop");
                Set<Location> explosionRooms = clips.collectActionPhase("explosion");
                if (!flammableRooms.isEmpty() || !machineryDamageRooms.isEmpty() || !explosionRooms.isEmpty()) {
                    Set<Location> fireLineLocs = clips.getFireLineLocations();
                    for (Location loc : fireLineLocs) {
                        List<HydrantOutlets> hydrants = clips.getHydrantOutletsForLocation(loc);
                        processFireLineHydrants(loc, hydrants);
                    }
                    addPreventionFlammableRows(flammableRooms);
                    addPreventionMachineryDamageRows(machineryDamageRooms);
                    addPreventionExplosionRows(explosionRooms);
                }
            }
            case FIREFIGHTING -> addFireExtinguishingRows(clips.getFirefightingPlanPairs());
        }
        actionsTableModel.fireTableDataChanged();
    }

    private void clearActionsTable() {
        actionsTableModel.clear();
    }

    private void addEvacuationRows(Set<Location> locations) {
        for (Location location : locations) {
            String room = location.getCode().toUpperCase();
            String message = String.format(Localization.get("message.evacuation.room"), room);
            actionsTableModel.addRow(SolutionPhase.EVACUATION, actionsTableRowCount() + 1, room, message);
        }
    }

    private void addSealingVentilationRows(Set<Location> locations) {
        for (Location location : locations) {
            String room = location.getCode().toUpperCase();
            String message = String.format(Localization.get("message.sealing.ventilation"), room);
            actionsTableModel.addRow(SolutionPhase.SEALING, actionsTableRowCount() + 1, room, message);
        }
    }

    private void addSealingDoorCloseRows(List<Link> doors, boolean inclusive) {
        int limit = doors.size() - (inclusive ? 0 : 1);
        for (int n = 0; n < limit; n++) {
            Link door = doors.get(n);
            String code = door.getCode();
            if (code.charAt(0) < code.charAt(1)) {
                String doorIn = door.getFrom().getCode().toUpperCase();
                String doorOut = door.getTo().getCode().toUpperCase();
                String message = String.format(Localization.get("message.sealing.door.close"), doorIn, doorOut);
                actionsTableModel.addRow(SolutionPhase.SEALING, actionsTableRowCount() + 1, new DoorLabel(door).toDisplayString(), message);
            }
        }
    }

    private void addSealingDoorKeepOpenRows(List<Link> doors) {
        int limit = doors.size() - 1;
        for (int n = 0; n < limit; n++) {
            Link door = doors.get(n);
            String code = door.getCode();
            if (code.charAt(0) < code.charAt(1)) {
                String doorIn = door.getFrom().getCode().toUpperCase();
                String doorOut = door.getTo().getCode().toUpperCase();
                String message = String.format(Localization.get("message.sealing.door.close.with.hose"), doorIn, doorOut);
                actionsTableModel.addRow(SolutionPhase.SEALING, actionsTableRowCount() + 1, new DoorLabel(door).toDisplayString(), message);
            }
        }
    }

    private void addPreventionFlammableRows(Set<Location> locations) {
        for (Location location : locations) {
            String room = location.getCode().toUpperCase();
            String message = String.format(Localization.get("message.prevention.flammable"), room);
            actionsTableModel.addRow(SolutionPhase.PREVENTION_OF_FLAMMABLE, actionsTableRowCount() + 1, room, message);
        }
    }

    private void addPreventionMachineryDamageRows(Set<Location> locations) {
        for (Location location : locations) {
            String room = location.getCode().toUpperCase();
            String message = String.format(Localization.get("message.prevention.machinery.damage"), room);
            actionsTableModel.addRow(SolutionPhase.MACHINERY_DAMAGE, actionsTableRowCount() + 1, room, message);
        }
    }

    private void addPreventionExplosionRows(Set<Location> locations) {
        for (Location location : locations) {
            String room = location.getCode().toUpperCase();
            String message = String.format(Localization.get("message.prevention.explosion"), room);
            actionsTableModel.addRow(SolutionPhase.PREVENTION_OF_EXPLOSION, actionsTableRowCount() + 1, room, message);
        }
    }

    private void addFireExtinguishingRows(Map<Location, Location> roomToFromLocation) {
        if (roomToFromLocation != null) {
            for (Map.Entry<Location, Location> entry : roomToFromLocation.entrySet()) {
                Location room = entry.getKey();
                Location fromLocation = entry.getValue();
                String message = (fromLocation != null)
                    ? String.format(Localization.get("message.firefighting"), fromLocation.getCode().toUpperCase())
                    : noComputedRouteMessage(room);
                actionsTableModel.addRow(SolutionPhase.FIREFIGHTING, actionsTableRowCount() + 1, room.getCode().toUpperCase(), message);
            }
        }
    }

    /**
     * {@code room} is on fire but CLIPS computed no door-to-door route to it (a real, confirmed
     * scenario — see {@code clips.ClipsEngineAccess#getStepFrom}'s javadoc): every door-adjacent
     * neighbor was either cut off (e.g. itself being evacuated) or, per the room's only remaining
     * door, leads into a different compartment. A link crossing compartments is always a door with
     * no matching border (borders never span compartments — see {@code topology.yaml}), so a tank
     * mismatch on the other side of a link reliably identifies that case; name its tank number
     * instead of leaving the recommendation blank.
     */
    private String noComputedRouteMessage(Location room) {
        return findCrossCompartmentNeighbor(room)
            .map(neighbor -> String.format(Localization.get("message.firefighting.no.access"),
                room.getCode().toUpperCase(), neighbor.getTank()))
            .orElseGet(() -> String.format(Localization.get("message.firefighting.no.plan"), room.getCode().toUpperCase()));
    }

    private Optional<Location> findCrossCompartmentNeighbor(Location room) {
        return topology.allLinks().stream()
            .filter(link -> link.getFrom().equals(room) || link.getTo().equals(room))
            .map(link -> link.getOtherSide(room))
            .filter(neighbor -> neighbor.getTank() != room.getTank())
            .findFirst();
    }

    private int actionsTableRowCount() {
        return actionsTableModel.getRowCount();
    }

    /**
     * Routes a right-clicked actions-table row to the {@link ClipsReadOnlyService} explanation
     * method matching its {@link SolutionPhase} — the "why" behind that row's recommendation, shown
     * in {@link ActionsTableMouseAdapter}'s popup. A phase with no explanation support (e.g.
     * {@code LOCALIZATION}, {@code FIREFIGHTING}) falls through to {@link Explanation#EMPTY}.
     */
    private Explanation resolveExplanationFromClips(SolutionPhase phase, String roomCode) {
        return switch (phase) {
            case SEALING -> resolveSealingExplanation(roomCode);
            case EVACUATION -> clips.getExplanationForEvacuation(topology.location(roomCode));
            case PREVENTION_OF_EXPLOSION -> clips.getExplanationForExplosions(topology.location(roomCode));
            case PREVENTION_OF_FLAMMABLE -> clips.getExplanationForFlammable(topology.location(roomCode));
            case MACHINERY_DAMAGE -> clips.getExplanationForMachineryDamage(topology.location(roomCode));
            default -> Explanation.EMPTY;
        };
    }

    /**
     * A {@code SEALING} row is either a room (single-character code, ventilation) or a door (the
     * four-character {@code "A, B"} {@link DoorLabel} display string) — the two share a phase but
     * resolve through different {@code ClipsReadOnlyService} methods.
     */
    private Explanation resolveSealingExplanation(String roomCode) {
        if (roomCode.length() == 1) {
            return clips.getExplanationForLocation(topology.location(roomCode));
        } else {
            DoorLabel doorLabel = DoorLabel.fromDisplayString(roomCode, topology);
            return doorLabel == null ? Explanation.EMPTY : clips.getExplanationForDoorSealing(doorLabel.link());
        }
    }

    /**
     * Same row lookup + routing {@link ActionsTableMouseAdapter#mousePressed} performs, minus the
     * popup-display side effect — {@link JPopupMenu#show} requires the table to be showing on
     * screen and throws {@code IllegalComponentStateException} headless, so this is the only way to
     * exercise the routing decision (which {@code getExplanationFor*} method a given row maps to)
     * from a headless test.
     */
    @VisibleForTesting
    Explanation resolveExplanationForRow(int row) {
        SolutionPhase phase = actionsTableModel.getPhaseSection(row);
        String roomCode = actionsTable.getValueAt(row, 2).toString().toLowerCase();
        return resolveExplanationFromClips(phase, roomCode);
    }

    private void processFireLineHydrants(Location location, List<HydrantOutlets> hydrants) {
        if (hydrants == null || hydrants.isEmpty())
            return;

        for (HydrantOutlets hydrant : hydrants) {
            String message = String.format(Localization.get("message.localization.hydrant"), hydrant.getTitle());
            actionsTableModel.addRow(SolutionPhase.LOCALIZATION, actionsTableRowCount() + 1, location.getCode().toUpperCase(), message);
        }
    }

    /**
     * Mirrors {@code IMMEDIATE-EXTINGUISHERS::use-local} in feis.clp (kept as a console printout,
     * unchanged): recommends every unused portable extinguisher at each fire location, wording
     * chosen by its agent type.
     */
    private void addExtinguisherRows(Set<Location> fireLocations) {
        for (Location location : fireLocations) {
            for (Extinguisher extinguisher : clips.getExtinguishersForLocation(location)) {
                String key = extinguisher.getType() == ExtinguisherType.CARBON_DIOXIDE
                    ? "message.extinguisher.co" : "message.extinguisher.af";
                String message = String.format(Localization.get(key), extinguisher.getTitle());
                actionsTableModel.addRow(SolutionPhase.IMMEDIATE_MEASURES, actionsTableRowCount() + 1, location.getCode().toUpperCase(), message);
            }
        }
    }

    private static JTable createEventsTable(EventsTableModel eventsTableModel) {
        final JTable eventsTable = new JTable(eventsTableModel);
        eventsTable.setSelectionMode(0);
        eventsTable.getColumnModel().getColumn(0).setMaxWidth(20);
        eventsTable.getColumnModel().getColumn(1).setWidth(140);
        eventsTable.getColumnModel().getColumn(2).setMaxWidth(35);
        eventsTable.getColumnModel().getColumn(3).setMaxWidth(60);
        eventsTable.getColumnModel().getColumn(4).setMaxWidth(60);
        return eventsTable;
    }

    private static JTable createActionsTable(ActionsTableModel actionsTableModel) {
        final JTable actionsTable = new JTable(actionsTableModel);
        actionsTable.setSelectionMode(0);
        actionsTable.getColumnModel().getColumn(0).setMinWidth(60);
        actionsTable.getColumnModel().getColumn(1).setMaxWidth(20);
        actionsTable.getColumnModel().getColumn(2).setMaxWidth(35);
        actionsTable.getColumnModel().getColumn(3).setMinWidth(370);
        return actionsTable;
    }

    private static class ActionsTableModel extends AbstractTableModel {
        private final List<ActionRow> rows = new ArrayList<>();

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> Localization.get("action.column.phase");
                case 1 -> Localization.get("label.number");
                case 2 -> Localization.get("action.column.room");
                case 3 -> Localization.get("action.column.recommendation");
                default -> null;
            };
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ActionRow actionRow = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> toSolutionPhaseDisplayName(actionRow.phase());
                case 1 -> actionRow.num();
                case 2 -> actionRow.room();
                case 3 -> actionRow.recommendation();
                default -> null;
            };
        }

        private static String toSolutionPhaseDisplayName(SolutionPhase phase) {
            return switch (phase) {
                case EVACUATION -> TITLE_EVACUATION;
                case SEALING -> TITLE_SEALING;
                case PREVENTION_OF_FLAMMABLE -> TITLE_PREVENTION_OF_FLAMMABLE;
                case MACHINERY_DAMAGE -> TITLE_MACHINERY_DAMAGE;
                case PREVENTION_OF_EXPLOSION -> TITLE_PREVENTION_OF_EXPLOSION;
                case LOCALIZATION -> TITLE_LOCALIZATION;
                case FIREFIGHTING -> TITLE_FIREFIGHTING;
                case IMMEDIATE_MEASURES -> TITLE_IMMEDIATE_MEASURES;
            };
        }

        public void addRow(SolutionPhase phase, int num, String room, String recommendation) {
            rows.add(new ActionRow(phase, num, room, recommendation));
        }

        public SolutionPhase getPhaseSection(int rowIndex) {
            return rows.get(rowIndex).phase();
        }

        public void clear() {
            rows.clear();
        }

        private record ActionRow(SolutionPhase phase, int num, String room, String recommendation) {}
    }

    private static class EventsTableModel extends AbstractTableModel {
        private final List<EventRow> rows = new ArrayList<>();

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> Localization.get("label.number");
                case 1 -> Localization.get("event.column.datetime");
                case 2 -> Localization.get("event.column.room");
                case 3 -> Localization.get("event.column.deck");
                case 4 -> Localization.get("event.column.compartment");
                case 5 -> Localization.get("event.column.type");
                default -> null;
            };
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            EventRow eventRow = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> eventRow.num();
                case 1 -> eventRow.dateTime();
                case 2 -> eventRow.room();
                case 3 -> eventRow.deck();
                case 4 -> eventRow.compartment();
                case 5 -> eventRow.event();
                default -> null;
            };
        }

        public void addRow(int num, String dateTime, String room, String deck, String compartment, String event) {
            rows.add(new EventRow(num, dateTime, room, deck, compartment, event));
        }

        private record EventRow(int num, String dateTime, String room, String deck, String compartment, String event) {}
    }

    /**
     * A genuine (non-static) inner class — reaches {@link SolutionResultsController#clips}/{@link
     * SolutionResultsController#topology} via the enclosing instance rather than holding its own
     * copies, since {@link #resolveExplanationFromClips} now lives on the outer class (see its
     * javadoc for why: {@link #resolveExplanationForRow} needs the same routing without the
     * popup-display side effect below, and cannot reach a {@code private static} nested class's
     * per-instance method).
     */
    private class ActionsTableMouseAdapter extends MouseAdapter {
        private final JTable actionsTable;
        private final JPopupMenu popupMenu;

        public ActionsTableMouseAdapter(JTable actionsTable, JPopupMenu popupMenu) {
            this.actionsTable = actionsTable;
            this.popupMenu = popupMenu;
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            int selectedRow = actionsTable.getSelectedRow();

            SolutionPhase phase = actionsTableModel.getPhaseSection(selectedRow);
            String roomCode = actionsTable.getValueAt(selectedRow, 2).toString().toLowerCase();

            Explanation clipsExplanation = resolveExplanationFromClips(phase, roomCode);
            if (clipsExplanation.isPresent()) {
                Rectangle rectangle = actionsTable.getCellRect(selectedRow, 3, true);
                popupMenu.removeAll();
                popupMenu.add(new JMenuItem(clipsExplanation.toHtml()));
                popupMenu.show(actionsTable, rectangle.x, rectangle.y);
            }
        }
    }

    private record DoorLabel(Link link) {
        public DoorLabel(Link link) {
            this.link = Objects.requireNonNull(link);
        }

        public String toDisplayString() {
            return link.getFrom().getCode().toUpperCase() + ", " + link.getTo().getCode().toUpperCase();
        }

        public static DoorLabel fromDisplayString(String displayString, TopologyModel topology) {
            if (displayString != null && displayString.length() == 4) {
                String fromCode = displayString.substring(0, 1).toLowerCase();
                String toCode = displayString.substring(3, 4).toLowerCase();
                return new DoorLabel(topology.link(fromCode + toCode));
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return toDisplayString();
        }
    }

    private enum SolutionPhase {
        EVACUATION, SEALING, PREVENTION_OF_FLAMMABLE, MACHINERY_DAMAGE, PREVENTION_OF_EXPLOSION, LOCALIZATION,
        FIREFIGHTING, IMMEDIATE_MEASURES
    }
}
