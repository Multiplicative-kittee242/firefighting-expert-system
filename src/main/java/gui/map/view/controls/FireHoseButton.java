package gui.map.view.controls;

import java.awt.Graphics;

public class FireHoseButton extends HydrantToggleButton {
    private final String title;

    public FireHoseButton(String title) {
        this.title = title;
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        drawTitle(g, title);
        if (getToggleSize() == 1) {
            g.drawOval(50, 4, 6, 6);
            g.drawLine(50, 4, 56, 10);
            g.drawLine(56, 4, 50, 10);
            g.drawOval(58, 4, 6, 6);
            g.drawLine(58, 4, 64, 10);
            g.drawLine(64, 4, 58, 10);
        } else {
            g.drawOval(16, 16, 6, 6);
            g.drawLine(16, 16, 22, 22);
            g.drawLine(22, 16, 16, 22);
            g.drawOval(24, 16, 6, 6);
            g.drawLine(24, 16, 30, 22);
            g.drawLine(30, 16, 24, 22);
        }
        drawLeftVerticalStripe(g);
    }
}
