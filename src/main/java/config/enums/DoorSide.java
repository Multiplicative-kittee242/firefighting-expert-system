package config.enums;

/**
 * Which side of a {@code vertical} door's glyph the hinge / handle icon is drawn on, see
 * {@code gui.map.input.controls.DoorSealingButton#drawVerticalDoor}. Not used for a fire-rated door (its glyph ignores
 * the side entirely) — {@code groups.yaml} omits {@code align} there, which binds to {@code null}.
 */
public enum DoorSide {
    LEFT,
    RIGHT
}
