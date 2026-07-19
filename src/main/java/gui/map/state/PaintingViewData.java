package gui.map.state;

import domain.Link;
import domain.Location;

import java.util.List;
import java.util.Set;

public record PaintingViewData(
    Set<Location> fireLocations,
    Set<Location> threatenedLocations,
    List<Link> fireLineLinks
) {
    public static final PaintingViewData EMPTY = new PaintingViewData(Set.of(), Set.of(), List.of());
}
