package config.groups;

/**
 * Stable keys for on-map control / label groups: {@code assembly.yaml} layer entries, optional group construction in
 * the deck-map assembler, and phase-driven show / hide ({@code gui.map.MapLayerVisibilityManager}). Names match the
 * configuration vocabulary.
 */
public enum GroupKey {
    FIRE_SENSOR_GROUP,
    FIRE_BUTTON_GROUP,
    EVACUATION_GROUP,
    VENTILATION_GROUP,
    DOOR_SEALING_GROUP,
    EXPLOSION_GROUP,
    FLAMMABLE_GROUP,
    MACHINERY_DAMAGE_GROUP,
    FRONTLINE_BALANCE_GROUP,
    HYDRANT_OUTLETS_GROUP,
    FIREFIGHTING_STEPS_GROUP,
    EXTINGUISHERS_GROUP,

    FIRE_HOSE,
    HYDR_EXT,
    HYDR_EXT_B,
    HYDR_EXT_B_FROM
}
