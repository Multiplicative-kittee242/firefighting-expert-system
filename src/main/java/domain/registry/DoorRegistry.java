package domain.registry;

import domain.Door;
import domain.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only in-memory registry of {@link Door} value objects, built once from a raw {@code (from, to)} list, resolved
 * against a {@link LocationRegistry}. {@code to} may be the {@link Location#OUT} code instead of a real location code,
 * meaning the door exits the modeled deck entirely rather than connecting to another mapped compartment — see
 * {@link Door#getTo()}.
 */
public final class DoorRegistry {
    private final List<Door> all;

    public DoorRegistry(List<RawDoor> rawDoors, LocationRegistry locations) {
        List<Door> doors = new ArrayList<>();
        for (RawDoor raw : rawDoors) {
            Location from = locations.get(raw.from());
            Location to = Location.OUT.getCode().equalsIgnoreCase(raw.to()) ? Location.OUT : locations.get(raw.to());
            doors.add(new Door(from, to));
        }
        this.all = List.copyOf(doors);
    }

    /**
     * @return an unmodifiable view of all registered doors, in insertion order.
     */
    public List<Door> all() {
        return Collections.unmodifiableList(all);
    }

    /**
     * A raw {@code (from, to)} pair prior to endpoint resolution, independent of any config / Jackson type so this
     * registry has no dependency on the {@code config} package. {@code to} is either a location code or the
     * {@link Location#OUT} code.
     */
    public record RawDoor(String from, String to) {}
}
