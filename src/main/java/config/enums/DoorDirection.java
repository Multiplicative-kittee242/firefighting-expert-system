package config.enums;

/**
 * Which end of a door's glyph the hinge / handle icon is drawn at, see
 * {@code gui.map.input.controls.DoorSealingButton#drawVerticalDoor}/{@code #drawHorizontalDoor}. Not used for a
 * fire-rated {@code HORISONTAL} door (its glyph ignores both side and direction) — {@code groups.yaml} omits
 * {@code valign} there, which binds to {@code null}.
 */
public enum DoorDirection {
    TOP,
    BOTTOM
}
