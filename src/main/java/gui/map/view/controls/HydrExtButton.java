package gui.map.view.controls;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class HydrExtButton extends HydrantToggleButton {
    private final String title;

    public HydrExtButton(String title) {
        this.title = title;
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        Font font = g.getFont();
        g.setColor(Color.BLACK);
        g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
        g.drawString(title, 27, 11);
        g.setFont(font);

        g.drawLine(4, 6, 4, 10);
        g.drawLine(4, 8, 18, 8);
        g.drawLine(12, 6, 18, 8);
        g.drawLine(12, 10, 18, 8);
        g.drawOval(19, 7, 2, 2);
        drawLeftVerticalStripe(g);
    }
}
