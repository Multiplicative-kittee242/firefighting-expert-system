package gui.map.input;

import domain.types.FlammableMaterial;
import gui.actions.InputAction;
import config.groups.ToggleGroupConfig;
import config.specification.ElementPlacement;
import config.specification.buttons.FlammableButtonSpec;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.InputControlListener;
import gui.map.state.InputControlsData;
import gui.map.input.controls.PreventionButton;
import domain.types.PreventionType;

import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

public class FlammableButtonGroup extends AbstractToggleGroup<PreventionButton, Location> implements InputControlListener {

    public FlammableButtonGroup(ToggleGroupConfig<FlammableButtonSpec> config, TopologyModel topology) {
        super(config.items().stream()
                .map(s -> new ElementPlacement<>(topology.location(s.locationCode()), s.position()))
                .toList(),
            config.items().stream()
                .map(s -> new PreventionButton(flammableTypeFor(topology.location(s.locationCode()))))
                .toList(),
            config.size().width(),
            config.size().height()
        );
    }

    private static PreventionType flammableTypeFor(Location location) {
        FlammableMaterial material = location.getBurningMaterial()
            .orElseThrow(() -> new IllegalStateException("Location " + location.getCode() + " has a prevention button but no flammable material"));
        return switch (material) {
            case MACHINE_OIL -> PreventionType.OIL;
            case WORKING_CLOTHES -> PreventionType.CLOTHES;
        };
    }

    @Override
    public void onInputControlsDataChanged(InputControlsData data) {
        for (Location location : data.flammableLocations())
            setVisibleFor(location);
    }

    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((location, button) -> attachInputAction(button, new InputAction.FlammableActionInput(location, button.getType())));
    }

    public Map<Location, PreventionType> collectChanges(Location targetLocation, PreventionType preventionType) {
        Map<Location, PreventionType> preventionChanges = new LinkedHashMap<>();
        PreventionButton control = getControlFor(targetLocation);
        if (control != null) {
            boolean selected = control.isSelected();
            preventionChanges.put(targetLocation, selected ? PreventionType.DONE : preventionType);
        }
        return preventionChanges;
    }
}
