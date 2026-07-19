package clips.values.internal;

/**
 * CLIPS wire tokens for flammable-material prevention reports ({@code done}/{@code pump_out}/{@code carry_out}).
 * {@code clips.values.internal} tier — only {@code clips} and {@code gui.actions} may reference this type; GUI code
 * holds {@code domain.types.PreventionType} and remaps via {@code ClipsValuesMapper}. One-way Java → CLIPS; no
 * parse-back.
 */
public enum FlammablePreventionClipsAction {
    DONE("done"),
    PUMP_OUT("pump_out"),
    CARRY_OUT("carry_out");

    private final String clipsValue;

    FlammablePreventionClipsAction(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }
}
