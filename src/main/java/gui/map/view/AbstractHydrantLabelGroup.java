package gui.map.view;

import config.specification.ElementPlacement;
import gui.map.input.AbstractControlGroup;
import gui.map.state.HydrantViewListener;

import javax.swing.JLabel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for on-map label groups whose items are {@link JLabel} subclasses
 * rather than toggle buttons.
 * Owns reset ({@link #resetAll}) and data-update
 * ({@link #putData}) logic shared by {@link HydrantOutletsGroup}, {@link FirefightingStepGroup},
 * and {@link FrontlineBalanceGroup}.
 * <p>
 * {@code D} is the domain key type of this group's elements; see {@link AbstractControlGroup}.
 * {@link HydrantOutletsGroup} fixes it to {@link domain.HydrantOutlets}, while
 * {@link FrontlineBalanceGroup} and {@link FirefightingStepGroup} fix it to {@link domain.Location}.
 */
public abstract class AbstractHydrantLabelGroup<T extends JLabel, D> extends AbstractControlGroup<T, D> implements HydrantViewListener {
    private final Map<D, T> controlByKey = new LinkedHashMap<>();

    protected AbstractHydrantLabelGroup(List<ElementPlacement<D>> elements, List<T> controls) {
        super(controls, elements);

        for (int i = 0; i < controls.size(); i++) {
            ElementPlacement<D> placement = elements.get(i);
            T previous = controlByKey.put(placement.key(), controls.get(i));
            if (previous != null)
                throw new IllegalStateException("Configuration key initialized twice: " + placement.key());
        }
    }

    protected void resetAll() {
        for (T item : getControls()) {
            item.setVisible(false);
            item.setEnabled(false);
        }
    }

    /**
     * Shows the label for {@code key} (if this group has one) and delegates content update
     * to {@link #updateLabel}. {@code data2} defaults to {@code key}'s string form when null.
     */
    protected void putData(D key, String data1, String data2) {
        T label = getLabelFor(key);
        if (label != null) {
            label.setVisible(false);
            label.setEnabled(true);
            updateLabel(label, data1, data2 != null ? data2 : String.valueOf(key));
        }
    }

    protected T getLabelFor(D key) {
        return controlByKey.get(key);
    }

    /**
     * Writes display data into a single label.
     */
    protected abstract void updateLabel(T label, String data1, String data2);
}
