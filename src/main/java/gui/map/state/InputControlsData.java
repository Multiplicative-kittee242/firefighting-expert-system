package gui.map.state;

import domain.Link;
import domain.Location;

import java.util.List;
import java.util.Set;

public record InputControlsData(
    Set<Location> evacuationLocations,
    Set<Location> ventilationOffLocations,
    List<Link> sealingDoorsToClose,
    List<Link> sealingDoorsKeepOpen,
    Set<Location> flammableLocations,
    Set<Location> machineryDamageLocations
) {
    public static final InputControlsData EMPTY = new InputControlsData(Set.of(), Set.of(), List.of(), List.of(), Set.of(), Set.of());
}
