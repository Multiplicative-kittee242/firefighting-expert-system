package gui.map.view.controls;

import gui.map.ColorPalette;
import config.enums.HydrantLabelSize;
import gui.Localization;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class FrontlineBalanceLabel extends JLabel {
    private final HydrantLabelSize size;

    private int here;
    private int total;

    public FrontlineBalanceLabel(int here, int total, HydrantLabelSize size) {
        this.here = here;
        this.total = total;
        this.size = size;
    }

    public void setNumbers(int here, int need) {
        this.here = here;
        this.total = here + need;
    }

    public HydrantLabelSize getLabelSize() {
        return size;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (here != total) {
            g.setColor(ColorPalette.RED);
            g.fillRect(0, 0, getSize().width, getSize().height);

            g.setColor(ColorPalette.DARK_RED);
            g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
        } else {
            g.setColor(ColorPalette.GREEN);
            g.fillRect(0, 0, getSize().width, getSize().height);

            g.setColor(ColorPalette.DARK_GREEN);
            g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
        }

        Font font = g.getFont();
        g.setColor(Color.BLACK);
        g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
        String format = Localization.get(size.getTitleFormatKey());
        int x = size == HydrantLabelSize.FULL ? 4 : 3;
        g.drawString(String.format(format, here, total), x, 13);
        g.setFont(font);
    }
}
