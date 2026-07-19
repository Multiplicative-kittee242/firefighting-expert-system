package gui.map.input.controls;

import gui.map.ColorPalette;
import domain.types.ExplosiveType;

import java.awt.Color;
import java.awt.Graphics;

public class ExplosionButton extends AbstractToggleButton {
    private final ExplosiveType type;

    public ExplosionButton(ExplosiveType type) {
        this.type = type;
    }

    public ExplosiveType getType() {
        return type;
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        if (type == ExplosiveType.AIR || type == ExplosiveType.REAGENT) {
            if (type == ExplosiveType.AIR)
                g.setColor(ColorPalette.SKY_BLUE);
            if (type == ExplosiveType.REAGENT)
                g.setColor(ColorPalette.CO_GRAY);
            g.fillRect(4, 6, 11, 10);
            g.drawLine(5, 3, 5, 6);
            g.drawLine(9, 3, 9, 6);
            g.drawLine(13, 3, 13, 6);

            if (type == ExplosiveType.AIR)
                g.setColor(Color.WHITE);
            if (type == ExplosiveType.REAGENT)
                g.setColor(Color.YELLOW);
            g.drawLine(4, 8, 14, 8);

            g.setColor(Color.BLACK);
            g.drawRoundRect(3, 5, 4, 11, 2, 2);
            g.drawRoundRect(7, 5, 4, 11, 2, 2);
            g.drawRoundRect(11, 5, 4, 11, 2, 2);
            g.drawLine(3, 16, 15, 16);
            g.drawRoundRect(4, 2, 2, 3, 2, 2);
            g.drawRoundRect(8, 2, 2, 3, 2, 2);
            g.drawRoundRect(12, 2, 2, 3, 2, 2);
        } else {
            g.setColor(ColorPalette.BROWN);
            g.fillRect(3, 5, 13, 11);

            g.setColor(getBackgroundFillColor(selected));
            g.fillRect(4, 6, 11, 9);

            g.setColor(ColorPalette.BROWN);
            g.drawLine(15, 5, 3, 15);
        }
    }
}
