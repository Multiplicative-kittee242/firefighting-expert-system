package gui.map.input.controls;

import gui.map.ColorPalette;

import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * Base class for the custom toggle buttons drawn on the deck map.
 * Handles the common selected/unselected fill and border painting,
 * delegating element-specific symbol drawing to subclasses via {@link #drawContent}.
 */
public abstract class AbstractToggleButton extends JToggleButton {

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBorderPainted(false);
        boolean selected = isSelected();

        g.setColor(selected ? getSelectedFillColor() : getUnselectedFillColor());
        g.fillRect(0, 0, getSize().width, getSize().height);

        g.setColor(selected ? getSelectedBorderColor() : getUnselectedBorderColor());
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);

        drawContent(g, selected);
    }

    protected Color getBackgroundFillColor(boolean selected) {
        return selected ? ColorPalette.GREEN : ColorPalette.RED;
    }

    protected Color getSelectedFillColor() {
        return ColorPalette.GREEN;
    }

    protected Color getSelectedBorderColor() {
        return ColorPalette.DARK_GREEN;
    }

    protected Color getUnselectedFillColor() {
        return ColorPalette.RED;
    }

    protected Color getUnselectedBorderColor() {
        return ColorPalette.DARK_RED;
    }

    protected void drawContent(Graphics g, boolean selected) {
        // no-op by default
    }

    /**
     * Draws a filled rectangle with a border.
     */
    protected static void drawFilledRectWithBorder(Graphics g, int x, int y, int width, int height,
                                                   Color fillColor, Color borderColor) {
        g.setColor(fillColor);
        g.fillRect(x, y, width, height);
        g.setColor(borderColor);
        g.drawRect(x, y, width - 1, height - 1);
    }

    /**
     * Draws a thin vertical stripe on the left (2 pixels wide).
     */
    protected void drawLeftVerticalStripe(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 2, getSize().height);
    }

    /**
     * Draws {@code text} horizontally centered within a {@code width}-pixel-wide button, at
     * baseline {@code y}. Width-aware (via {@link FontMetrics}) so labels of varying length
     * (single-letter glyphs in most locales, two-letter ones like {@code "R1"}/{@code "WT"} in
     * others — see {@code domain.types.FireSensorType}/{@code VentilationType}) all center correctly
     * under whatever font is currently set on {@code g}.
     */
    protected static void drawCenteredString(Graphics g, String text, int width, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int x = (width - metrics.stringWidth(text) + 1) / 2;
        g.drawString(text, x, y);
    }
}
