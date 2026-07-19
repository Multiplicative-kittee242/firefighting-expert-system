package clips.values;

/**
 * Operator-reported door state for {@link clips.ClipsReportService#reportDoorSealingChanges}: wire tokens
 * {@code open}/{@code close}. Plain {@code clips.values} tier — safe for GUI code without remapping. One-way Java →
 * CLIPS; no parse-back. Not the recommendation statuses {@code to-close}/{@code keep-open} returned by the read path.
 */
public enum DoorState {
    OPEN("open"),
    CLOSE("close");

    private final String clipsValue;

    DoorState(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }
}
