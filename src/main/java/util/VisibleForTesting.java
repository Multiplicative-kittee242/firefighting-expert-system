package util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a member whose visibility is wider than production code needs, solely so a test in another package can reach it
 * directly (e.g. a {@code protected} member widened to public).
 * <p>
 * The annotation itself does nothing at compile time or runtime — the JVM enforces access purely through the declared
 * modifier, regardless of any annotation present. Existing libraries with a same-named annotation (Guava's
 * {@code com.google.common.annotations.VisibleForTesting}, AndroidX's, Error Prone's) are exactly as inert as this one;
 * pulling in any of them — Guava in particular — would mean a whole new dependency (plus a {@code gradle.lockfile}
 * entry) for a marker with zero runtime behavior. Rolled our own instead: same documentation value, no new dependency.
 * <p>
 * {@code RetentionPolicy.CLASS} (not the more common {@code SOURCE}): this needs to survive into the compiled
 * {@code .class} file so {@code ArchitectureRulesTest} can see it via bytecode analysis — {@code SOURCE} retention is
 * stripped by {@code javac} and would be invisible there. {@code CLASS} still carries no actual runtime footprint (not
 * reflectively visible via {@code Class#getAnnotations()} at JVM runtime — only tools reading the class file directly,
 * like ArchUnit, see it), so ordinary production code pays nothing for it either way.
 * <p>
 * Enforced, not just documented: {@code src/test/java/architecture/ArchitectureRulesTest.java}'s
 * {@code visibleForTestingMembersAreOnlyUsedFromTestCode} rule fails the build if any production class calls a member
 * annotated with this.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.CONSTRUCTOR})
public @interface VisibleForTesting {
}
