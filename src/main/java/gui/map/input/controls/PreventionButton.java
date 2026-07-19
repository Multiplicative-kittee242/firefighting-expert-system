package gui.map.input.controls;

import domain.types.PreventionType;
import gui.map.ColorPalette;

import java.awt.Color;
import java.awt.Graphics;

public class PreventionButton extends AbstractToggleButton {
    private final PreventionType type;

    public PreventionButton(PreventionType type) {
        this.type = type;
    }

    public PreventionType getType() {
        return type;
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        if (type == PreventionType.OIL) {
            g.setColor(ColorPalette.BROWN);
            g.fillRect(3, 5, 13, 11);

            g.setColor(getBackgroundFillColor(selected));
            g.fillRect(4, 6, 11, 9);
        }
        if (type == PreventionType.MECHANICAL) {
            g.setColor(Color.BLACK);
            g.fillRect(8, 3, 3, 13);
            g.fillRect(3, 8, 13, 3);
            g.drawLine(5, 5, 13, 13);
            g.drawLine(5, 4, 14, 13);
            g.drawLine(4, 5, 13, 14);
            g.drawLine(13, 5, 5, 13);
            g.drawLine(13, 4, 4, 13);
            g.drawLine(14, 5, 5, 14);
            g.fillOval(4, 4, 11, 11);

            g.setColor(getBackgroundFillColor(selected));
            g.fillOval(5, 5, 9, 9);

            g.setColor(Color.BLACK);
            g.fillOval(8, 8, 3, 3);
            g.drawLine(6, 6, 6, 6);
            g.drawLine(6, 12, 6, 12);
        }
        if (type == PreventionType.CLOTHES) {
            g.setColor(Color.BLACK);
            g.drawLine(2, 8, 16, 8);
            g.drawLine(2, 6, 8, 5);
            g.drawLine(10, 5, 16, 6);
            g.drawLine(8, 6, 10, 6);
            g.drawLine(2, 7, 2, 7);
            g.drawLine(16, 7, 16, 7);
            g.fillRect(6, 8, 7, 6);

            g.setColor(getBackgroundFillColor(selected));
            g.fillRect(7, 7, 5, 6);
        }
    }
}
