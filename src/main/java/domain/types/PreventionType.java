package domain.types;

/**
 * GUI-side choice of which flammable / machinery-prevention object a map button targets (and the letter painted on that
 * button). This is <em>not</em> a CLIPS wire token and not {@link FlammableMaterial} (compartment identity seeded from
 * {@code topology.yaml}).
 * <p>
 * For flammable materials, reported actions are remapped by
 * {@code gui.actions.ClipsValuesMapper#toClips(PreventionType)} into
 * {@code clips.values.internal.FlammablePreventionClipsAction}. {@link #MECHANICAL} is the machinery-damage button
 * glyph (routed via {@code clips.values.MachineryDamageAction}, not the flammable mapper). {@link #DONE} is the
 * selected-toggle marker when the operator finished the action; {@link #getCode()} is the on-button letter (empty for
 * {@link #DONE}).
 */
public enum PreventionType {
    OIL("O"),
    CLOTHES("C"),
    MECHANICAL("M"),
    DONE("");

    private final String code;

    PreventionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
