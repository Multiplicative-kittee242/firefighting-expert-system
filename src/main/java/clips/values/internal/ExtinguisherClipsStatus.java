package clips.values.internal;

/**
 * CLIPS wire tokens for portable-extinguisher used status ({@code yes}/{@code no}). Internal tier despite a trivial
 * mapping from {@code gui.map.values.ExtinguisherUsage} — {@code used} is a real engine guard (
 * {@code IMMEDIATE-EXTINGUISHERS::use-local} in {@code feis.clp}). Only {@code clips} and {@code gui.actions} may
 * reference this type. One-way Java → CLIPS; no parse-back.
 */
public enum ExtinguisherClipsStatus {
    USED("yes"),
    NOT_USED("no");

    private final String clipsValue;

    ExtinguisherClipsStatus(String clipsValue) {
        this.clipsValue = clipsValue;
    }

    public String getClipsValue() {
        return clipsValue;
    }
}
