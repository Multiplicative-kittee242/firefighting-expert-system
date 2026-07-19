package gui.map.input;

import gui.actions.InputAction;
import config.specification.ElementPlacement;

import javax.swing.JToggleButton;
import java.awt.event.ActionListener;
import java.util.List;

public abstract class AbstractToggleGroup<T extends JToggleButton, D> extends AbstractControlGroup<T, D> implements ActionListenerSupport {
    private final int width;
    private final int height;

    protected AbstractToggleGroup(List<ElementPlacement<D>> elements, List<T> items, int width, int height) {
        super(items, elements);
        this.width = width;
        this.height = height;
    }

    @Override
    public void addActionListener(ActionListener listener) {
        for (T control : getControls())
            control.addActionListener(listener);
    }

    protected void attachInputAction(T button, InputAction action) {
        if (button != null && action != null)
            button.putClientProperty(InputAction.INPUT_ACTION_PROPERTY, action);
    }

    protected void setVisibleFor(D key) {
        T control = getControlFor(key);
        if (control != null) {
            control.setVisible(true);
            control.setEnabled(true);
            control.setSelected(false);
        }
        setVisible(true);
    }

    @Override
    protected int getControlWidth(JToggleButton control) {
        return width;
    }

    @Override
    protected int getControlHeight(JToggleButton control) {
        return height;
    }
}
