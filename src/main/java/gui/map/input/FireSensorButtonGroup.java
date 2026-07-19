package gui.map.input;

import gui.actions.InputAction;
import config.groups.ToggleGroupConfig;
import config.specification.ElementPlacement;
import config.specification.buttons.FireSensorButtonSpec;
import domain.FireSensor;
import domain.registry.TopologyModel;
import gui.map.input.controls.FireSensorButton;

import java.awt.event.ActionListener;

public class FireSensorButtonGroup extends AbstractToggleGroup<FireSensorButton, FireSensor> {

    public FireSensorButtonGroup(ToggleGroupConfig<FireSensorButtonSpec> config, TopologyModel topology) {
        super(config.items().stream()
                .map(s -> new ElementPlacement<>(topology.fireSensor(s.sensorCode()), s.position()))
                .toList(),
            config.items().stream()
                .map(s -> new FireSensorButton(topology.fireSensor(s.sensorCode()).getType(), config.size().width()))
                .toList(),
            config.size().width(),
            config.size().height()
        );
    }

    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((sensor, button) -> attachInputAction(button, new InputAction.FireActionInput(sensor.getLocation())));
    }

    @Override
    public void setVisible(boolean visible) {
        for (FireSensorButton sensor : getControls()) {
            if (visible || !sensor.isSelected())
                sensor.setVisible(visible);
        }
    }
}
