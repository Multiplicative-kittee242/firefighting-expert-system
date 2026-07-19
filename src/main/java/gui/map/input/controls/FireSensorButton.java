package gui.map.input.controls;

import gui.Localization;
import domain.types.FireSensorType;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class FireSensorButton extends AbstractStickyFireButton {
    private final String letter;
    private final int letterWidth;

    public FireSensorButton(FireSensorType type, int letterWidth) {
        this.letter = Localization.get(shortKey(type));
        this.letterWidth = letterWidth;
        setToolTipText(Localization.get(fullKey(type)));
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        Font font = g.getFont();
        g.setColor(Color.BLACK);
        g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
        drawCenteredString(g, letter, letterWidth, 9);
        g.setFont(font);

        g.setColor(Color.BLACK);
        g.fillOval(6, 10, 7, 7);
        g.drawLine(2, 13, 16, 13);
    }

    private static String shortKey(FireSensorType type) {
        return "label.sensor." + keySuffix(type) + ".short";
    }

    private static String fullKey(FireSensorType type) {
        return "label.sensor." + keySuffix(type) + ".full";
    }

    private static String keySuffix(FireSensorType type) {
        return type.name().toLowerCase().replace('_', '-');
    }
}
