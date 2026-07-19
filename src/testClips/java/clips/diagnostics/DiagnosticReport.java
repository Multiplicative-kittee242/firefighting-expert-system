package clips.diagnostics;

import java.util.Objects;

/**
 * Minimal self-contained assertion/report helper for the standalone {@code main()}-based checks in
 * this package. These cannot be regular JUnit tests: they need the real 32-bit CLIPSJNI native
 * library and the portable JRE (see {@code runApp}/{@code runClipsDiagnostic} in {@code build.gradle}),
 * neither of which the normal {@code test} task's 64-bit JVM can load (see
 * {@code src/test/java/clips/ClipsEngineAccessTest}'s javadoc for the same constraint).
 * <p>
 * Every check prints a {@code [OK]}/{@code [FAIL]} line as it runs (so a hang or crash mid-run still
 * leaves a readable trail) and {@link #finish()} exits the JVM with a non-zero status if anything
 * failed, so these are safe to wire into a CI step later via their exit code alone.
 */
public final class DiagnosticReport {
    private int passed = 0;
    private int failed = 0;

    void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [OK]   " + description);
        } else {
            failed++;
            System.out.println("  [FAIL] " + description);
        }
    }

    void checkEquals(String description, Object expected, Object actual) {
        check(description + " (expected " + expected + ", got " + actual + ")", Objects.equals(expected, actual));
    }

    /** Prints the pass/fail tally and exits with status 1 if anything failed. Call exactly once, last. */
    void finish() {
        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0)
            System.exit(1);
    }
}
