package gui.actions;

import javax.swing.AbstractButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Reads the {@link InputAction} attached to a clicked button's client property (see
 * {@link InputAction#INPUT_ACTION_PROPERTY}) and forwards it to an {@link ActionDispatcher}.
 * <p>
 * {@code dispatcher} is settable after construction rather than passed to a constructor:
 * {@code gui.map.DeckMapController} needs an {@code ActionListener} at construction time (to
 * attach to every button it builds), but {@link ActionDispatcher} itself needs the already-built
 * {@code DeckMapController} — so this listener is built first, wired to every button via the
 * controller's constructor, and only then given its dispatcher once one exists.
 */
public class InputActionListener implements ActionListener {
    private ActionDispatcher dispatcher;

    public void setDispatcher(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof AbstractButton button) {
            Object actionObj = button.getClientProperty(InputAction.INPUT_ACTION_PROPERTY);
            if (actionObj instanceof InputAction inputAction)
                dispatcher.dispatch(inputAction);
        }
    }
}
