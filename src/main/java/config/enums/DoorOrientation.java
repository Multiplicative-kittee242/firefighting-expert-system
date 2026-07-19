package config.enums;

/**
 * Whether a door's sealing glyph is drawn tall (a bulkhead door) or wide (a deck / tank hatch). See
 * {@code gui.map.input.controls.DoorSealingButton#drawContent}. Always required — every door has one.
 */
public enum DoorOrientation {
    VERTICAL,
    HORISONTAL
}
