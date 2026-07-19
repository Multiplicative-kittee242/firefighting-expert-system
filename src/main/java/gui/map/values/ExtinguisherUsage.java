package gui.map.values;

/**
 * GUI-facing state of a single extinguisher toggle button: whether the operator has marked it
 * used. Remapped to {@code clips.values.internal.ExtinguisherClipsStatus} before being reported
 * to CLIPS — this type never crosses into the {@code clips}/{@code app} packages directly.
 */
public enum ExtinguisherUsage {
    USED,
    NOT_USED
}
