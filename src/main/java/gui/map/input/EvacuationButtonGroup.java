package gui.map.input;

import gui.actions.InputAction;
import clips.values.EvacuationStatus;
import config.specification.ElementPlacement;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.InputControlListener;
import gui.map.state.InputControlsData;
import gui.map.input.controls.EvacuationButton;

import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-contained group that owns the evacuation buttons (evac),
 * their placement on the map, action listeners and visibility state.
 */
public class EvacuationButtonGroup extends AbstractToggleGroup<EvacuationButton, Location> implements InputControlListener {
    public EvacuationButtonGroup(List<ElementPlacement<String>> elements, TopologyModel topology) {
        super(elements.stream()
                .map(e -> new ElementPlacement<>(topology.location(e.key()), e.point()))
                .toList(),
            elements.stream()
                .map(e -> new EvacuationButton())
                .toList(),
            19, 19
        );
    }

    @Override
    public void onInputControlsDataChanged(InputControlsData data) {
        for (Location location : data.evacuationLocations())
            setVisibleFor(location);
    }

    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((location, button) -> attachInputAction(button, new InputAction.EvacuationActionInput(location)));
    }

    /**
     * Collects pending evacuation status changes for all buttons placed at
     * {@code targetLabel}, as currently selected by the user. A selected button
     * reports {@link EvacuationStatus#DONE} and is disabled; an unselected one
     * reports {@link EvacuationStatus#NONE}.
     */
    public Map<Location, EvacuationStatus> collectChanges(Location targetLocation) {
        Map<Location, EvacuationStatus> evacChanges = new LinkedHashMap<>();
        EvacuationButton control = getControlFor(targetLocation);
        if (control != null) {
            EvacuationStatus status = control.isSelected() ? EvacuationStatus.DONE : EvacuationStatus.NONE;
            evacChanges.put(targetLocation, status);
            if (status == EvacuationStatus.DONE)
                control.setEnabled(false);
        }
        return evacChanges;
    }
}
