package gui.map.input;

import gui.actions.InputAction;
import config.specification.ElementPlacement;
import domain.Extinguisher;
import domain.registry.TopologyModel;
import gui.map.input.controls.ExtinguisherButton;
import gui.map.values.ExtinguisherUsage;

import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-contained group that owns the extinguisher-used buttons, their placement on the map,
 * action listeners and visibility state — shaped exactly like the other toggle groups (e.g.
 * {@link EvacuationButtonGroup}) so it is ready to receive real button placements later.
 * <p>
 * Currently always constructed with an empty {@code elements} list (no {@code topology.yaml}
 * coordinates exist yet for extinguishers), so no button is ever created and every method here
 * is a no-op in practice: {@link #getControlFor} always returns {@code null}. The group is also
 * deliberately left out of {@code MapLayerVisibilityManager}'s phase rules, so even if elements
 * were added it would still default to hidden on every phase change — this feature has no user
 * feedback path yet by design.
 */
public class ExtinguisherButtonGroup extends AbstractToggleGroup<ExtinguisherButton, Extinguisher> {
    public ExtinguisherButtonGroup(List<ElementPlacement<String>> elements, TopologyModel topology) {
        super(elements.stream()
                .map(e -> new ElementPlacement<>(topology.extinguisher(e.key()), e.point()))
                .toList(),
            elements.stream()
                .map(e -> new ExtinguisherButton())
                .toList(),
            19, 19
        );
    }

    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((extinguisher, button) -> attachInputAction(button, new InputAction.ExtinguisherActionInput(extinguisher)));
    }

    @Override
    public void setVisible(boolean visible) {
        for (ExtinguisherButton button : getControls())
            button.setVisible(visible);
    }

    /**
     * Collects the pending used/not-used status for the button placed at {@code targetExtinguisher},
     * as currently selected by the user. A selected button reports {@link ExtinguisherUsage#USED}
     * and is disabled; an unselected one reports {@link ExtinguisherUsage#NOT_USED}.
     */
    public Map<Extinguisher, ExtinguisherUsage> collectChanges(Extinguisher targetExtinguisher) {
        Map<Extinguisher, ExtinguisherUsage> changes = new LinkedHashMap<>();
        ExtinguisherButton control = getControlFor(targetExtinguisher);
        if (control != null) {
            ExtinguisherUsage status = control.isSelected() ? ExtinguisherUsage.USED : ExtinguisherUsage.NOT_USED;
            changes.put(targetExtinguisher, status);
            if (status == ExtinguisherUsage.USED)
                control.setEnabled(false);
        }
        return changes;
    }
}
