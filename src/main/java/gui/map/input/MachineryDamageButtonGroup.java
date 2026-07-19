package gui.map.input;

import domain.types.PreventionType;
import gui.actions.InputAction;
import clips.values.MachineryDamageAction;
import config.groups.ToggleGroupConfig;
import config.specification.ElementPlacement;
import config.specification.buttons.MachineryDamageButtonSpec;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.InputControlListener;
import gui.map.state.InputControlsData;
import gui.map.input.controls.PreventionButton;

import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

public class MachineryDamageButtonGroup extends AbstractToggleGroup<PreventionButton, Location> implements InputControlListener {

    public MachineryDamageButtonGroup(ToggleGroupConfig<MachineryDamageButtonSpec> config, TopologyModel topology) {
        super(config.items().stream()
                .map(s -> new ElementPlacement<>(topology.location(s.locationCode()), s.position()))
                .toList(),
            config.items().stream()
                .map(s -> new PreventionButton(PreventionType.MECHANICAL))
                .toList(),
            config.size().width(),
            config.size().height()
        );
    }

    @Override
    public void onInputControlsDataChanged(InputControlsData data) {
        for (Location location : data.machineryDamageLocations())
            setVisibleFor(location);
    }

    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((location, button) -> attachInputAction(button, new InputAction.MachineryDamageActionInput(location)));
    }

    public Map<Location, MachineryDamageAction> collectChanges(Location targetLocation) {
        Map<Location, MachineryDamageAction> preventionChanges = new LinkedHashMap<>();
        PreventionButton control = getControlFor(targetLocation);
        if (control != null) {
            MachineryDamageAction action = control.isSelected() ? MachineryDamageAction.DONE : MachineryDamageAction.STOP;
            preventionChanges.put(targetLocation, action);
        }
        return preventionChanges;
    }
}
