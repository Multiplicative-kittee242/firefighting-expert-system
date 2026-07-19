package domain;

import java.util.Objects;

/**
 * Immutable CLIPS-reported firefighting plan reference for a single room on fire: the room the firefighting route
 * arrives {@code from}, and the sequential {@code stepNumber} of this room within the overall firefighting plan.
 */
public record FirefightingStep(Location from, int stepNumber) {

    public FirefightingStep {
        Objects.requireNonNull(from, "from must not be null");
        if (stepNumber <= 0)
            throw new IllegalArgumentException("stepNumber must be positive");
    }
}
