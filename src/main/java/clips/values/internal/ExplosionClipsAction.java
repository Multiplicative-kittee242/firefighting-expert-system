package clips.values.internal;

/**
 * CLIPS wire tokens for explosion-prevention reports (
 * {@code done}/{@code carry_out}/{@code pump_out}/{@code to_fight}). {@code clips.values.internal} tier — only
 * {@code clips} and {@code gui.actions} may reference this type; GUI code holds {@code domain.types.ExplosiveType} and
 * remaps via {@code ClipsValuesMapper}. One-way Java → CLIPS; no parse-back.
 */
public enum ExplosionClipsAction {
    DONE("done"),
    CARRY_OUT("carry_out"),
    PUMP_OUT("pump_out"),
    TO_FIGHT("to_fight");

    private final String clipsValue;

    ExplosionClipsAction(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }
}
