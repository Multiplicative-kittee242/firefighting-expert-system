package clips.values;

/**
 * Operator-reported machinery-damage prevention choice for
 * {@link clips.ClipsReportService#reportMachineryDamagePreventionChanges}. Plain {@code clips.values} tier, but unlike
 * siblings it has <em>no</em> {@code clipsValue} string: {@link #DONE} and {@link #STOP} select different
 * {@code ClipsEngineAccess} methods rather than interpolating a slot token. One-way Java → CLIPS; no parse-back.
 */
public enum MachineryDamageAction {
    STOP,
    DONE
}
