package gui.map.input;

import gui.actions.InputAction;
import gui.actions.InputAction.FireActionInput;
import config.specification.ElementPlacement;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.input.controls.FireButton;

import java.awt.event.ActionListener;
import java.util.List;

/**
 * Self-contained group that owns the fire call-point buttons (fire_btn),
 * their placement on the map, action listeners and visibility state.
 */
public class FireButtonGroup extends AbstractToggleGroup<FireButton, Location> {

    public FireButtonGroup(List<ElementPlacement<String>> elements, TopologyModel topology) {
        super(elements.stream()
                .map(e -> new ElementPlacement<>(topology.location(e.key()), e.point()))
                .toList(),
            elements.stream()
                .map(e -> new FireButton())
                .toList(),
            11, 11
        );
    }

    /**
     * Registers the shared {@link ActionListener} on every button, then attaches a typed
     * {@link FireActionInput} so the click is dispatched via {@link gui.actions.ActionDispatcher}.
     */
    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((location, button) -> attachInputAction(button, new InputAction.FireActionInput(location)));
    }

    @Override
    public void setVisible(boolean visible) {
        for (FireButton button : getControls()) {
            if (visible || !button.isSelected())
                button.setVisible(visible);
        }
    }
}
