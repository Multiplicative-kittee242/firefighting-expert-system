package gui.map.view.controls;

import gui.map.ColorPalette;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class HydrantOutletsLabel extends JLabel {
    private final int size;

    private int free;
    private String title;

    public HydrantOutletsLabel(int free, int size) {
        this.free = free;
        this.size = size;
    }

    public void setNumbers(String free, String title) {
        this.free = Integer.parseInt(free);
        this.title = title;
    }

    public int getLabelSize() {
        return size;
    }

    @Override
    public void paintComponent(Graphics g) {
        int x;
        int i;
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getSize().width, getSize().height);

        g.setColor(ColorPalette.MID_DARK_RED);
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);

        for (i = 0; i < free; ++i) {
            x = i * 14;
            g.fillOval(x + 2, 7, 13, 13);
            g.fillRect(x + 4, 2, 9, 3);
            g.drawLine(x + 3, 3, x + 13, 3);
            g.fillRect(x + 7, 4, 3, 3);
        }
        if (size != free) {
            g.setColor(ColorPalette.BLUE);
            for (i = 0; i < size - free; ++i) {
                x = (i + free) * 14;
                g.fillOval(x + 2, 7, 13, 13);
                g.fillRect(x + 4, 2, 9, 3);
                g.drawLine(x + 3, 3, x + 13, 3);
                g.fillRect(x + 7, 4, 3, 3);
            }
        }

        Font font = g.getFont();
        g.setColor(ColorPalette.MID_DARK_RED);
        g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 4));
        if (size == 1) {
            g.drawLine(3, 21, 3, 26);
            g.drawLine(4, 23, 4, 23);
            g.drawLine(5, 24, 5, 26);
            g.drawLine(7, 27, 9, 27);
            g.drawString(title.substring(5), 10, 27);
        } else {
            g.drawLine(3, 21, 3, 26);
            g.drawLine(4, 23, 4, 23);
            g.drawLine(5, 24, 5, 26);
            g.drawLine(7, 23, 7, 25);
            g.drawLine(8, 26, 8, 26);
            g.drawLine(9, 23, 9, 27);
            g.drawLine(8, 28, 8, 28);
            g.drawLine(11, 24, 11, 25);
            g.drawLine(12, 23, 12, 23);
            g.drawLine(12, 26, 12, 26);
            g.drawLine(13, 21, 13, 26);
            g.drawLine(15, 23, 15, 26);
            g.drawLine(16, 24, 16, 24);
            g.drawLine(17, 23, 17, 23);
            g.drawLine(17, 27, 18, 27);
            g.drawString(title.substring(5), 20, 27);
        }
        g.setFont(font);
    }
}
