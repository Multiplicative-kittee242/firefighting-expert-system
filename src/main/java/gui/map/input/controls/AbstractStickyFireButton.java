package gui.map.input.controls;

import gui.map.ColorPalette;

import javax.swing.SwingUtilities;
import java.awt.Color;

public class AbstractStickyFireButton extends AbstractToggleButton {

    public AbstractStickyFireButton() {
        addChangeListener(e -> {
            if (isSelected()) {
                SwingUtilities.invokeLater(() -> {
                    if (isSelected())
                        setEnabled(false);
                });
            }
        });
    }

    @Override
    protected Color getSelectedFillColor() {
        return ColorPalette.RED;
    }

    @Override
    protected Color getSelectedBorderColor() {
        return ColorPalette.DARK_RED;
    }

    @Override
    protected Color getUnselectedFillColor() {
        return ColorPalette.LIGHT_PASTELLE_GREY;
    }

    @Override
    protected Color getUnselectedBorderColor() {
        return Color.BLACK;
    }
}
