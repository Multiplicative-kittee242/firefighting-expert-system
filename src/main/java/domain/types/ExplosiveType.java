package domain.types;

/**
 * GUI-side choice of which explosive-prevention object a map button targets (and the letter painted on that button).
 * This is <em>not</em> a CLIPS wire token and not {@link ExplosiveMaterial} (compartment identity seeded from
 * {@code topology.yaml}).
 * <p>
 * Reported actions are remapped by {@code gui.actions.ClipsValuesMapper#toClips(ExplosiveType)} into
 * {@code clips.values.internal.ExplosionClipsAction} (
 * {@code carry_out}/{@code pump_out}/{@code to_fight}/{@code done}). {@link #DONE} is the selected-toggle marker
 * (operator finished the action); the material constants identify which button was used when the toggle is off.
 * <p>
 * {@link #getActionCommandSuffix()} is a leftover from the pre- {@code InputAction} string {@code actionCommand} era
 * and is unused by production code today.
 */
public enum ExplosiveType {
    AIR("A", "air"),
    OIL("O", "oil"),
    REAGENT("R", "rgt"),
    DONE("", ""); // marker value

    private final String letter;
    private final String actionCommandSuffix;

    ExplosiveType(String letter, String actionCommandSuffix) {
        this.letter = letter;
        this.actionCommandSuffix = actionCommandSuffix;
    }

    public String getLetter() {
        return letter;
    }

    /**
     * Legacy string suffix from the old {@code actionCommand} protocol; unused by current dispatch ({@code InputAction}
     * + {@code ClipsValuesMapper}).
     */
    public String getActionCommandSuffix() {
        return actionCommandSuffix;
    }
}
