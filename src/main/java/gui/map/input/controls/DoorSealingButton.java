package gui.map.input.controls;

import gui.map.ColorPalette;
import config.enums.DoorDirection;
import gui.map.values.DoorKeepType;
import config.enums.DoorOrientation;
import config.enums.DoorSide;

import java.awt.Color;
import java.awt.Graphics;

public class DoorSealingButton extends AbstractToggleButton {
    private final String doorCode;
    private final boolean fireRated;
    private final DoorOrientation doorOrientation;
    private final DoorSide doorSide;
    private final DoorDirection doorDirection;

    private DoorKeepType doorKeepType;

    public DoorSealingButton(String doorCode, boolean fireRated,
        DoorOrientation doorOrientation, DoorSide doorSide, DoorDirection doorDirection, DoorKeepType doorKeepType)
    {
        this.doorCode = doorCode;
        this.fireRated = fireRated;
        this.doorOrientation = doorOrientation;
        this.doorSide = doorSide;
        this.doorDirection = doorDirection;
        this.doorKeepType = doorKeepType;
    }

    public String getDoorCode() {
        return doorCode;
    }

    public boolean isFireRated() {
        return fireRated;
    }

    /**
     * Sets whether this door must be kept forcibly open (e.g. for a hose).
     */
    public void setKeepOpen(DoorKeepType keepOpenType) {
        this.doorKeepType = keepOpenType;
    }

    @Override
    protected Color getUnselectedFillColor() {
        return (doorKeepType == DoorKeepType.NO) ? ColorPalette.RED : ColorPalette.ORANGE;
    }

    @Override
    protected Color getUnselectedBorderColor() {
        return (doorKeepType == DoorKeepType.NO) ? ColorPalette.DARK_RED : ColorPalette.DARK_ORANGE;
    }

    private Color getDoorFillColor(boolean selected) {
        if (selected) {
            return ColorPalette.GREEN;
        } else if (doorKeepType == DoorKeepType.NO) {
            return ColorPalette.RED;
        } else {
            return ColorPalette.ORANGE;
        }
    }

    private Color getDoorBorderColor(boolean selected) {
        if (selected) {
            return ColorPalette.DARK_GREEN;
        } else if (doorKeepType == DoorKeepType.NO) {
            return ColorPalette.DARK_RED;
        } else {
            return ColorPalette.DARK_ORANGE;
        }
    }

    @Override
    protected void drawContent(Graphics g, boolean selected) {
        if (doorOrientation == DoorOrientation.VERTICAL) {
            drawVerticalDoor(g, selected);
        } else {
            drawHorizontalDoor(g, selected);
        }
    }

    private void drawVerticalDoor(Graphics g, boolean selected) {
        if (!fireRated) {
            if (doorSide == DoorSide.RIGHT) {
                drawVerticalNonFireRightDoor(g, selected);
            } else {
                drawVerticalNonFireLeftDoor(g, selected);
            }
        } else {
            drawVerticalFireDoor(g, selected);
        }
    }

    private void drawVerticalNonFireRightDoor(Graphics g, boolean selected) {
        g.setColor(Color.BLACK);
        if (doorDirection == DoorDirection.TOP) {
            if (!selected) {
                g.fillOval(-30, 1, 58, 58);
                g.setColor(getDoorFillColor(false));
                g.fillOval(-29, 2, 56, 56);
                g.fillRect(16, 0, getSize().width, getSize().height);
                g.setColor(Color.BLACK);
                g.drawLine(0, 30, 11, 4);
            } else {
                g.drawLine(2, 2, 3, 2);
                g.drawLine(3, 2, 3, 27);
                g.drawLine(2, 27, 3, 27);
                g.drawLine(4, 4, 8, 4);
            }
        } else if (!selected) {
            g.fillOval(-30, -29, 58, 58);
            g.setColor(getDoorFillColor(false));
            g.fillOval(-29, -28, 56, 56);
            g.fillRect(16, 0, getSize().width, getSize().height);
            g.setColor(Color.BLACK);
            g.drawLine(0, 0, 11, 25);
        } else {
            g.drawLine(2, 2, 3, 2);
            g.drawLine(3, 2, 3, 27);
            g.drawLine(2, 27, 3, 27);
            g.drawLine(4, 26, 8, 26);
        }
        g.setColor(getDoorBorderColor(selected));
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 2, getSize().height);
    }

    private void drawVerticalNonFireLeftDoor(Graphics g, boolean selected) {
        g.setColor(Color.BLACK);
        if (doorDirection == DoorDirection.TOP) {
            if (!selected) {
                g.fillOval(-10, 1, 58, 58);
                g.setColor(getDoorFillColor(false));
                g.fillOval(-9, 2, 56, 56);
                g.fillRect(0, 0, 2, getSize().height);
                g.setColor(Color.BLACK);
                g.drawLine(17, 28, 6, 4);
            } else {
                g.drawLine(14, 2, 15, 2);
                g.drawLine(14, 2, 14, 27);
                g.drawLine(14, 27, 15, 27);
                g.drawLine(13, 4, 9, 4);
            }
        } else if (!selected) {
            g.fillOval(-10, -29, 58, 58);
            g.setColor(getDoorFillColor(false));
            g.fillOval(-9, -28, 56, 56);
            g.fillRect(0, 0, 2, getSize().height);
            g.setColor(Color.BLACK);
            g.drawLine(17, 0, 6, 25);
        } else {
            g.drawLine(14, 2, 15, 2);
            g.drawLine(14, 2, 14, 27);
            g.drawLine(14, 27, 15, 27);
            g.drawLine(13, 26, 9, 26);
        }
        g.setColor(getDoorBorderColor(selected));
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
        g.setColor(Color.BLACK);
        g.fillRect(getSize().width - 2, 0, 2, getSize().height);
    }

    private void drawVerticalFireDoor(Graphics g, boolean selected) {
        g.setColor(Color.BLACK);
        if (doorDirection == DoorDirection.TOP) {
            if (!selected) {
                g.fillOval(-8, 2, 58, 58);
                g.setColor(getDoorFillColor(false));
                g.fillOval(-7, 3, 56, 56);
                g.fillRect(1, 1, 2, getSize().height - 1);
                g.setColor(Color.BLACK);
                g.drawLine(19, 29, 8, 6);
                g.drawLine(20, 27, 9, 6);
                g.drawLine(20, 29, 19, 26);
                g.drawLine(17, 23, 17, 23);
                g.drawLine(15, 19, 15, 19);
                g.drawLine(13, 15, 13, 15);
                g.drawLine(11, 11, 11, 11);
                g.drawLine(9, 7, 9, 7);
            } else {
                g.drawRect(18, 2, 2, 28);
                g.drawLine(17, 3, 13, 3);
                g.drawLine(19, 3, 19, 5);
                g.drawLine(19, 7, 19, 10);
                g.drawLine(19, 12, 19, 15);
                g.drawLine(19, 17, 19, 20);
                g.drawLine(19, 22, 19, 25);
                g.drawLine(19, 27, 19, 29);
            }
        } else if (!selected) {
            g.fillOval(-8, -26, 58, 58);
            g.setColor(getDoorFillColor(false));
            g.fillOval(-7, -25, 56, 56);
            g.fillRect(1, 1, 2, getSize().height - 1);
            g.setColor(Color.BLACK);
            g.drawLine(19, 4, 8, 27);
            g.drawLine(20, 5, 9, 28);
            g.drawLine(20, 4, 19, 6);
            g.drawLine(17, 10, 17, 10);
            g.drawLine(15, 14, 15, 14);
            g.drawLine(13, 18, 13, 18);
            g.drawLine(11, 22, 11, 22);
            g.drawLine(9, 26, 9, 26);
        } else {
            g.drawRect(18, 3, 2, 28);
            g.drawLine(17, 30, 13, 30);
            g.drawLine(19, 4, 19, 5);
            g.drawLine(19, 7, 19, 10);
            g.drawLine(19, 12, 19, 15);
            g.drawLine(19, 17, 19, 20);
            g.drawLine(19, 22, 19, 25);
            g.drawLine(19, 27, 19, 30);
        }
        g.setColor(getDoorBorderColor(selected));
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
        g.setColor(Color.BLACK);
        g.fillRect(getSize().width - 1, 0, 1, getSize().height);
    }

    private void drawHorizontalDoor(Graphics g, boolean selected) {
        if (!fireRated) {
            if (doorSide == DoorSide.LEFT)
                drawHorizontalNonFireLeftDoor(g, selected);
        } else {
            drawHorizontalFireDoor(g, selected);
        }
    }

    private void drawHorizontalNonFireLeftDoor(Graphics g, boolean selected) {
        g.setColor(Color.BLACK);
        if (doorDirection == DoorDirection.TOP) {
            if (!selected) {
                g.fillOval(1, -10, 58, 58);
                g.setColor(getDoorFillColor(false));
                g.fillOval(2, -9, 56, 56);
                g.fillRect(0, 0, getSize().width, 2);
                g.setColor(Color.BLACK);
                g.drawLine(4, 7, 28, 16);
            } else {
                g.drawLine(2, 14, 2, 15);
                g.drawLine(2, 14, 27, 14);
                g.drawLine(27, 14, 27, 15);
                g.drawLine(3, 9, 3, 13);
            }
        } else if (!selected) {
            g.fillOval(1, -30, 58, 58);
            g.setColor(getDoorFillColor(false));
            g.fillOval(2, -29, 56, 56);
            g.fillRect(0, 16, getSize().width, getSize().height);
            g.setColor(Color.BLACK);
            g.drawLine(4, 11, 30, 0);
        } else {
            g.drawLine(2, 2, 2, 2);
            g.drawLine(2, 3, 27, 3);
            g.drawLine(27, 2, 27, 3);
            g.drawLine(3, 4, 3, 8);
        }
        g.setColor(getDoorBorderColor(selected));
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
        g.setColor(Color.BLACK);
        if (doorDirection == DoorDirection.TOP) {
            g.fillRect(0, getSize().height - 2, getSize().width, 2);
        } else {
            g.fillRect(0, 0, getSize().width, 2);
        }
    }

    private void drawHorizontalFireDoor(Graphics g, boolean selected) {
        g.setColor(Color.BLACK);
        if (!selected) {
            g.fillOval(-26, -26, 58, 58);
            g.setColor(getDoorFillColor(false));
            g.fillOval(-25, -25, 56, 56);
            g.fillRect(1, getSize().height - 3, getSize().width, 2);
            g.setColor(Color.BLACK);
            g.drawLine(4, 3, 27, 15);
            g.drawLine(5, 2, 28, 14);
            g.drawLine(4, 2, 5, 3);
            g.drawLine(9, 5, 9, 5);
            g.drawLine(13, 7, 13, 7);
            g.drawLine(17, 9, 17, 9);
            g.drawLine(21, 11, 21, 11);
            g.drawLine(25, 13, 27, 14);
        } else {
            g.drawRect(3, 2, 28, 2);
            g.drawLine(30, 5, 30, 9);
            g.drawLine(4, 3, 5, 3);
            g.drawLine(7, 3, 10, 3);
            g.drawLine(12, 3, 15, 3);
            g.drawLine(17, 3, 20, 3);
            g.drawLine(22, 3, 25, 3);
            g.drawLine(27, 3, 30, 3);
        }
        g.setColor(getDoorBorderColor(selected));
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getSize().width, 2);
    }
}
