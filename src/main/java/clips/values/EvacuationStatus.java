package clips.values;

/**
 * Operator-reported evacuation status for {@link clips.ClipsReportService#reportEvacuationChanges}: wire tokens
 * {@code done}/{@code none}. Plain {@code clips.values} tier — safe for GUI code without remapping. One-way Java →
 * CLIPS; no parse-back.
 */
public enum EvacuationStatus {
    DONE("done"),
    NONE("none");

    private final String clipsValue;

    EvacuationStatus(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }
}
