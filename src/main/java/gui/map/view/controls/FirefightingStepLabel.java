package gui.map.view.controls;

import gui.map.ColorPalette;
import config.enums.HydrantLabelSize;

import javax.swing.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class FirefightingStepLabel extends JLabel {
    private final HydrantLabelSize size;

    private String from;
    private String number;

    public FirefightingStepLabel(String from, String number, HydrantLabelSize size) {
        this.from = from;
        this.number = number;
        this.size = size;
    }

    public void setLabels(String from, String number) {
        this.from = (from == null || from.isBlank()) ? "" : from.trim().toUpperCase();
        this.number = (number == null || number.isBlank()) ? "" : number.trim();
    }

    public HydrantLabelSize getLabelSize() {
        return size;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(ColorPalette.GREEN);
        g.fillRect(0, 0, getSize().width, getSize().height);

        g.setColor(ColorPalette.DARK_GREEN);
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);

        Font font = g.getFont();
        g.setColor(Color.BLACK);
        g.setFont(new Font(font.getFontName(), Font.BOLD, font.getSize()));
        if (size == HydrantLabelSize.FULL) {
            g.drawString(number, 5, 14);
            g.drawString(from, 33, 14);
            g.drawLine(14, 9, 28, 9);
            g.drawLine(14, 9, 20, 7);
            g.drawLine(14, 9, 20, 11);
        } else {
            g.drawString(number, 5, 14);
            g.setFont(new Font(font.getFontName(), Font.PLAIN, font.getSize() - 2));
            g.drawString(from, 23, 13);
            g.drawLine(14, 9, 20, 9);
            g.drawLine(16, 8, 16, 10);
        }
        g.setFont(font);
    }
}
