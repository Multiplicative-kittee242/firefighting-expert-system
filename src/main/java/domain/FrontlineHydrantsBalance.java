package domain;

/**
 * Immutable CLIPS-reported hydrant coverage for a single fire-line location.
 * <p>
 * When populated from the engine ({@code hydrants-here}/{@code hydrants-need} on a {@code fire-line-location} fact):
 * <ul>
 * <li> {@code here} — hydrants already allocated to this location by CLIPS routing</li> <li> {@code need} —
 * still-outstanding count (not a static total); decremented as {@code here} increases, so {@code here + need} equals
 * the original requirement</li>
 * </ul>
 * The map label reconstructs the total as {@code here + need}. See
 * {@code clips.ClipsEngineAccess#getFireLineHydrantsNeeded}.
 */
public record FrontlineHydrantsBalance(int here, int need) {
    public FrontlineHydrantsBalance {
        if (here < 0)
            throw new IllegalArgumentException("here cannot be negative: " + here);
        if (need < 0)
            throw new IllegalArgumentException("need cannot be negative: " + need);
    }

    /**
     * {@code max(0, need - here)} under a <em>total-required</em> reading of {@code need}. When this record is filled
     * from CLIPS, {@code need} is already the outstanding remainder, so the production UI does not use this helper (it
     * treats outstanding as {@code need} itself).
     */
    public int getDeficit() {
        return Math.max(0, need - here);
    }

    /**
     * {@code here >= need} under a <em>total-required</em> reading of {@code need}. When this record is filled from
     * CLIPS, satisfaction is {@code need == 0}; the production UI does not use this helper.
     */
    public boolean isSatisfied() {
        return here >= need;
    }
}
