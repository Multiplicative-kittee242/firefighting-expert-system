package gui.map.view;

import domain.Location;

import java.util.HashMap;
import java.util.Map;

public class HydrantOffsetState {
    private final Map<Location, Integer> offsetByLocation = new HashMap<>();

    public int getOffset(Location location) {
        return offsetByLocation.getOrDefault(location, 0);
    }

    public void setOffset(Location location, int offset) {
        offsetByLocation.put(location, offset);
    }

    public void reset() {
        offsetByLocation.clear();
    }
}
