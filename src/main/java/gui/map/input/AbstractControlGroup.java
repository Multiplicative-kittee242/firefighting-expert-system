package gui.map.input;

import geometry.Point;
import config.specification.ElementPlacement;
import domain.Location;

import gui.map.StaticallyVisible;
import util.VisibleForTesting;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Base class for all on-map control element groups.
 * <p>
 * Owns the controls array, coordinate layout from configuration,
 * and common visibility handling. Provides a domain-key to coordinate mapping
 * built once during construction.
 *
 * @param <T> the concrete control component type (e.g. JButton, JToggleButton)
 * @param <D> the domain key type used in {@link ElementPlacement} (usually {@link Location} or String)
 */
public abstract class AbstractControlGroup<T extends JComponent, D> implements StaticallyVisible {
    private final List<T> controls;
    private final List<ElementPlacement<D>> elements;
    private final Map<D, T> controlByKey = new LinkedHashMap<>();

    protected AbstractControlGroup(List<T> controls, List<ElementPlacement<D>> elements) {
        this.controls = new ArrayList<>(controls);
        this.elements = new ArrayList<>(elements);

        Set<D> keySet = new HashSet<>();
        for (int i = 0; i < this.controls.size(); i++) {
            D key = elements.get(i).key();
            if (!keySet.add(key))
                throw new IllegalStateException("Configuration key initialized twice: " + key);
            controlByKey.put(key, this.controls.get(i));
        }
    }

    @Override
    public void addToMap(JLabel mapLabel, boolean initiallyVisible) {
        for (int i = 0; i < controls.size(); i++) {
            T control = controls.get(i);
            mapLabel.add(control);
            Point coordinate = getPlacement(i).point();
            control.setBounds(coordinate.x(), coordinate.y(), getControlWidth(control), getControlHeight(control));
            control.setVisible(initiallyVisible);
            control.setEnabled(initiallyVisible);
        }
    }

    @Override
    public void show() {
        setVisible(true);
    }

    @Override
    public void hide() {
        setVisible(false);
    }

    public void setVisible(boolean visible) {
        for (T control : controls) {
            if (!visible || control.isEnabled())
                control.setVisible(visible);
        }
    }

    protected ElementPlacement<D> getPlacement(int index) {
        return elements.get(index);
    }

    protected int size() {
        return elements.size();
    }

    protected List<T> getControls() {
        return controls;
    }

    protected T getControlFor(D key) {
        return controlByKey.get(key);
    }

    /**
     * Walks every {@code (key, control)} pair in insertion order.
     * <p>
     * Production subclasses only need {@code protected} access (e.g. to attach an
     * {@code InputAction} in {@code addActionListener}). Visibility is {@code public} solely so
     * integration tests in sibling packages under {@code gui.map} can click every control without
     * living in {@code gui.map.input} — that widen is what {@link VisibleForTesting} documents.
     * Inherited calls from subclasses are intentional production use of the original protected
     * contract; only a call from an unrelated production class would be a real leak (see
     * {@code ArchitectureRulesTest.visibleForTestingMembersAreOnlyUsedFromTestCode}).
     */
    @VisibleForTesting
    public void forEachControl(BiConsumer<D, T> action) {
        controlByKey.forEach(action);
    }

    protected abstract int getControlWidth(T control);

    protected abstract int getControlHeight(T control);
}
