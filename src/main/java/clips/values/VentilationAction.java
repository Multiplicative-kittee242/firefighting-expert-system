package clips.values;

/**
 * Operator-reported ventilation action for {@link clips.ClipsReportService#reportVentilationChanges}: wire tokens
 * {@code on}/{@code off}. Plain {@code clips.values} tier — safe for GUI code without remapping. One-way Java → CLIPS;
 * no parse-back.
 */
public enum VentilationAction {
    ON("on"),
    OFF("off");

    private final String clipsValue;

    VentilationAction(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }
}
