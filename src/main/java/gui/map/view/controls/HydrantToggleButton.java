package gui.map.view.controls;

import gui.map.input.controls.AbstractToggleButton;
import gui.map.view.HydrantButtonGroup;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public abstract class HydrantToggleButton extends AbstractToggleButton {
    private int size;

    public void setSize(int size) {
        this.size = size;
    }

    protected int getToggleSize() {
        return size;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawContent(g, isSelected());
    }

    protected void drawTitle(Graphics g, String title) {
        Font font = g.getFont();
        g.setColor(Color.BLACK);
        g.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 3));
        if (getToggleSize() == 1) {
            g.drawString(title, 5, 11);
        } else {
            g.drawString(String.format(HydrantButtonGroup.BUTTON_TITLE_FORMAT, title.substring(5)), 4, 11);
        }
        g.setFont(font);
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        // empty default implementation; concrete drawing happens in subclasses
    }
}
