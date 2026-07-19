package gui.map.input.controls;

import gui.map.ColorPalette;

import java.awt.Color;
import java.awt.Graphics;

public class FireButton extends AbstractStickyFireButton {

    @Override
    protected Color getUnselectedBorderColor() {
        return ColorPalette.DARK_ORANGE;
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        g.setColor(selected ? Color.BLACK : ColorPalette.DARK_RED);
        g.drawLine(3, 8, 3, 8);
        g.drawLine(2, 5, 2, 7);
        g.drawLine(3, 4, 5, 2);
        g.drawLine(5, 3, 5, 4);
        g.drawLine(6, 5, 6, 6);
        g.drawLine(7, 3, 7, 4);
        g.drawLine(8, 5, 8, 7);
        g.drawLine(7, 8, 7, 8);

        g.setColor(selected ? ColorPalette.MID_DARK_RED : ColorPalette.PASTELLE_ORANGE);
        g.drawLine(3, 5, 3, 7);
        g.drawLine(4, 4, 4, 8);
        g.drawLine(5, 5, 5, 8);
        g.drawLine(6, 7, 6, 8);
        g.drawLine(7, 5, 7, 7);
    }
}
