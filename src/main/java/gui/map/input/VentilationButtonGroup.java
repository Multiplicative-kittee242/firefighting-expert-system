package gui.map.input;

import gui.actions.InputAction;
import clips.values.VentilationAction;
import config.groups.ToggleGroupConfig;
import config.specification.ElementPlacement;
import config.specification.buttons.VentilationButtonSpec;
import domain.Location;
import domain.types.VentilationType;
import domain.registry.TopologyModel;
import gui.map.state.InputControlListener;
import gui.map.state.InputControlsData;
import gui.map.input.controls.VentilationButton;

import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

public class VentilationButtonGroup extends AbstractToggleGroup<VentilationButton, Location> implements InputControlListener {

    public VentilationButtonGroup(ToggleGroupConfig<VentilationButtonSpec> config, TopologyModel topology) {
        super(config.items().stream()
                .map(s -> new ElementPlacement<>(topology.location(s.locationCode()), s.position()))
                .toList(),
            config.items().stream()
                .map(s -> new VentilationButton(ventilationTypeFor(topology.location(s.locationCode())), config.size().width()))
                .toList(),
            config.size().width(),
            config.size().height()
        );
    }

    private static VentilationType ventilationTypeFor(Location location) {
        return location.getVentilationType()
            .orElseThrow(() -> new IllegalStateException("Location " + location.getCode() + " has a ventilation button but no ventilation system"));
    }

    @Override
    public void onInputControlsDataChanged(InputControlsData data) {
        for (Location location : data.ventilationOffLocations())
            setVisibleFor(location);
    }

    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((location, button) -> attachInputAction(button, new InputAction.VentilationActionInput(location)));
    }

    public Map<Location, VentilationAction> collectChanges(Location targetLocation) {
        Map<Location, VentilationAction> ventilChanges = new LinkedHashMap<>();
        VentilationButton control = getControlFor(targetLocation);
        if (control != null) {
            VentilationAction status = control.isSelected() ? VentilationAction.OFF : VentilationAction.ON;
            ventilChanges.put(targetLocation, status);
        }
        return ventilChanges;
    }
}
