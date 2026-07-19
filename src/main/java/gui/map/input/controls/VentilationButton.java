package gui.map.input.controls;

import gui.Localization;
import domain.types.VentilationType;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class VentilationButton extends AbstractToggleButton {
    private final String letter;
    private final int letterWidth;

    public VentilationButton(VentilationType type, int letterWidth) {
        this.letter = Localization.get(shortKey(type));
        this.letterWidth = letterWidth;
        setToolTipText(Localization.get(fullKey(type)));
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        g.setColor(Color.BLACK);
        g.fillOval(1, 1, 17, 17);

        g.setColor(getBackgroundFillColor(selected));
        g.fillOval(2, 2, 15, 15);
        g.fillRect(1, 8, 17, 3);
        g.fillRect(8, 1, 3, 17);

        Font font = g.getFont();
        g.setColor(Color.BLACK);
        g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 2));
        drawCenteredString(g, letter, letterWidth + 1, 13);
        g.setFont(font);
    }

    private static String shortKey(VentilationType type) {
        return "label.ventilation." + keySuffix(type) + ".short";
    }

    private static String fullKey(VentilationType type) {
        return "label.ventilation." + keySuffix(type) + ".full";
    }

    private static String keySuffix(VentilationType type) {
        return type.name().toLowerCase().replace('_', '-');
    }
}
