package gui.map.input.controls;

import java.awt.Color;
import java.awt.Graphics;

public class EvacuationButton extends AbstractToggleButton {
    @Override
    protected void drawContent(Graphics g, boolean selected) {
        g.setColor(Color.BLACK);
        g.fillRect(7, 3, 9, 13);

        g.setColor(getBackgroundFillColor(selected));
        g.fillRect(9, 5, 5, 9);
        g.fillRect(7, 7, 2, 5);

        g.setColor(Color.BLACK);
        g.drawLine(3, 9, 10, 9);
        g.drawLine(4, 8, 4, 10);
        g.drawLine(5, 7, 5, 11);
    }
}
