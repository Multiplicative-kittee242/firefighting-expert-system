package gui.map;

import javax.swing.JLabel;

public interface StaticallyVisible extends Visible {

    void addToMap(JLabel mapLabel, boolean initiallyVisible);
}
