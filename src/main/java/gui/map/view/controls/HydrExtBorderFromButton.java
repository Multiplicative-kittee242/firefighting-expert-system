package gui.map.view.controls;

import java.awt.Graphics;

public class HydrExtBorderFromButton extends HydrantToggleButton {
    private final String title;

    public HydrExtBorderFromButton(String title) {
        this.title = title;
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        drawTitle(g, title);
        if (getToggleSize() == 1) {
            g.drawLine(46, 6, 46, 10);
            g.drawLine(46, 8, 60, 8);
            g.drawLine(54, 6, 60, 8);
            g.drawLine(54, 10, 60, 8);
            g.drawOval(61, 7, 2, 2);
        } else {
            g.drawLine(13, 17, 13, 21);
            g.drawLine(13, 19, 27, 19);
            g.drawLine(21, 17, 27, 19);
            g.drawLine(21, 21, 27, 19);
            g.drawOval(28, 18, 2, 2);
        }
    }
}
