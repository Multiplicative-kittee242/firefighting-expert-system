package architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import util.VisibleForTesting;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Structural rules for the project's package layout, enforced against compiled bytecode so a
 * violation fails the build instead of surfacing only in review — several of these leaks have
 * been introduced and corrected by hand more than once during the package reorganization.
 * <p>
 * Two independent concerns live here:
 * <ul>
 *   <li><b>Layer ordering.</b> The package DAG must flow inward only: the domain model, the
 *   configuration layer and the expert-system integration never reach up into the GUI
 *   ({@code gui}), and nothing at all reaches into the composition root ({@code app}), which
 *   holds only the entry point {@code Main}. The UI command layer lives in {@code gui.actions},
 *   so {@code gui.map.input} button groups create their {@code InputAction} commands within the same
 *   layer and no {@code gui <-> app} cycle exists:
 *   <pre>
 *   domain ← config ← clips ← gui ← app
 *                 ↘          ↙
 *                   geometry
 *   </pre>
 *   <li><b>The CLIPS wire-protocol boundary</b> documented in {@code clips/values/README.md}:
 *   enums in {@code clips.values.internal} (e.g. {@link clips.values.internal.FlammablePreventionClipsAction},
 *   {@link clips.values.internal.ExtinguisherClipsStatus}) are CLIPS wire-protocol values, not
 *   GUI-facing types, and must only ever be referenced from {@code clips} itself (which defines
 *   and reports them) or {@code gui.actions} (which remaps GUI values into them via
 *   {@code gui.actions.ClipsValuesMapper}). Every other package — in particular the rest of
 *   {@code gui.*} — must go through a {@code gui.map.values}/{@code domain.types} counterpart instead.
 *   <li><b>Test-only visibility.</b> A member widened beyond what production code needs (e.g.
 *   {@code protected} to {@code public}) purely so a test in another package can reach it is
 *   marked {@link VisibleForTesting} — see that annotation's javadoc for why it exists instead of
 *   a library dependency. Nothing under {@code app}/{@code clips}/{@code config}/{@code domain}/
 *   {@code geometry}/{@code gui}/{@code util} may actually call one; if it's only ever called from
 *   test code today, this rule keeps it that way instead of quietly becoming load-bearing.
 * </ul>
 */
class ArchitectureRulesTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("app", "clips", "config", "domain", "geometry", "gui", "util");

    @Test
    void domainDependsOnNoOtherProjectLayer() {
        noClasses().that().resideInAPackage("domain..")
            .should().dependOnClassesThat().resideInAnyPackage("config..", "clips..", "gui..", "app..")
            .because("the domain model is the innermost layer: configuration, the expert system and the UI depend on it, never the reverse")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void configurationDoesNotDependOnExpertSystemGuiOrApp() {
        noClasses().that().resideInAPackage("config..")
            .should().dependOnClassesThat().resideInAnyPackage("clips..", "gui..", "app..")
            .because("configuration only describes data that is resolved into the domain; it must not reach into the expert system or the UI")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void expertSystemDoesNotDependOnGuiOrApp() {
        noClasses().that().resideInAPackage("clips..")
            .should().dependOnClassesThat().resideInAnyPackage("gui..", "app..")
            .because("the CLIPS integration is consumed by the UI and wired by app; it must depend on neither")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void nothingDependsOnTheCompositionRoot() {
        noClasses().that().resideOutsideOfPackage("app..")
            .should().dependOnClassesThat().resideInAPackage("app..")
            .because("app is only the composition root / entry point (Main); nothing may depend on it — including the GUI")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void topLevelPackagesAreFreeOfCycles() {
        slices().matching("(*)..")
            .should().beFreeOfCycles()
            .because("the layers must stay an acyclic DAG so any future two-way dependency (as the former gui <-> app.actions) fails fast")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void clipsInternalValuesAreOnlyUsedByClipsOrGuiActions() {
        noClasses().that(DescribedPredicate.not(resideInAnyPackage("clips..", "gui.actions..")))
            .should().dependOnClassesThat().resideInAPackage("clips.values.internal..")
            .because("clips.values.internal enums are CLIPS wire-protocol values, not GUI-facing types — only clips "
                + "(which defines/reports them) and gui.actions (which remaps GUI values into them via ClipsValuesMapper) may reference them")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void visibleForTestingMembersAreOnlyUsedFromTestCode() {
        classes().should(new ArchCondition<>("not call, read or write a member annotated @VisibleForTesting from outside its own class") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaAccess<?> access : javaClass.getAccessesFromSelf()) {
                    // A class calling its own (possibly inherited) @VisibleForTesting member — e.g.
                    // every *ButtonGroup#addActionListener calling its inherited forEachControl —
                    // is the ordinary protected-level use the member already had before it was
                    // additionally widened to public for tests; only a call from some *other* class
                    // is evidence of the widened, test-only access path actually leaking into
                    // production. getOriginOwner() and getTarget().getOwner() are always equal for
                    // any such self-call, however many levels up the hierarchy the member is
                    // actually declared, since both resolve to the class whose code is executing.
                    if (access.getTarget().isAnnotatedWith(VisibleForTesting.class)
                        && !access.getOriginOwner().equals(access.getTarget().getOwner())) {
                        String message = String.format("%s accesses %s, which is @VisibleForTesting",
                            access.getOrigin().getFullName(), access.getTarget().getFullName());
                        events.add(SimpleConditionEvent.violated(access, message));
                    }
                }
            }
        })
            .because("a member marked @VisibleForTesting is only widened for test access, per its own javadoc; "
                + "some *other* production class relying on that widened access would defeat the point of marking it as such")
            .check(PRODUCTION_CLASSES);
    }
}
