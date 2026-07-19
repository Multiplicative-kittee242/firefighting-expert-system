package clips;

import domain.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests that exercise the real CLIPSJNI 32-bit native engine (via ExpertSystemService).
 * These are deliberately separate from unit tests under src/test (see ClipsEngineAccessTest javadoc).
 * <p>
 * Run with: ./gradlew testClips (requires the portable 32-bit JRE provisioned by ensurePortableJre).
 * <p>
 * Expected values are, wherever the underlying feis.clp rule is a simple set-membership condition,
 * derived here from the same topology.yaml the production code reads (see the helper methods below),
 * not copied as opaque literals from the golden baseline. Where a field depends on a dynamic
 * graph-search/allocation computation inside CLIPS (evacuation-route reachability, fire-hose routing,
 * hydrant allocation), reproducing it would mean reimplementing that search in Java — defeating the
 * point of testing the real engine — so those values are taken as known inputs/outputs, cross-checked
 * against the corresponding golden file under src/testClips/resources/clips/diagnostics/golden/scenario-*.txt,
 * with a comment explaining why.
 * <p>
 * <b>One shared {@code ExpertSystemService} for the whole class, reset between scenarios</b> — see
 * {@link IncidentReportLoopIntegrationTest}'s class javadoc for why (constructing one native
 * {@code Environment} per scenario reliably crashed the JVM once combined with that class in the
 * same process) and {@link ExpertSystemService#resetForNewScenario()} for the mechanism.
 * {@code @Execution(SAME_THREAD)} and {@code @ResourceLock} guard the same shared-mutable-state and
 * cross-class-concurrency concerns documented there.
 * <p>
 * <b>{@code getExplanationFor*} coverage lives here too</b> (see the tests below
 * {@link #reportFireIncident_BuildsTheExpectedSnapshotForEachScenario}), rather than in their own
 * class, for the same
 * one-{@code Environment}-per-process reason: a dedicated fourth {@code testClips} class was tried
 * first and empirically increased this suite's already-documented native
 * {@code EXCEPTION_ACCESS_VIOLATION} GC-finalizer flakiness (observed in one session: 6 crashes out
 * of 8 full-suite runs with four classes, versus the historical ~1-in-5-6 baseline with three) —
 * each class's own {@code @BeforeAll} constructs its own native {@code Environment}, and one more
 * such construction per process proportionally raises the odds of hitting that race. Reusing this
 * class's already-constructed {@code service} keeps the process at three constructions, not four.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = FireScenarios.CLIPS_ENGINE_RESOURCE, mode = ResourceAccessMode.READ_WRITE)
class IncidentReportFireIntegrationTest {
    private ExpertSystemService service;

    @BeforeAll
    void beforeAll() {
        service = FireScenarios.createNewExpertSystemService();
    }

    @BeforeEach
    void beforeEach() {
        service.resetForNewScenario();
    }

    static Stream<Arguments> reportFireIncident_BuildsTheExpectedSnapshotForEachScenario() {
        return FireScenarios.fireScenarios();
    }

    @ParameterizedTest(name = "reports fire at {0}")
    @MethodSource
    void reportFireIncident_BuildsTheExpectedSnapshotForEachScenario(Location fireLocation, List<Location> evacLocations, List<Link> keepOpenDoors,
        List<Link> fireLineLinks, List<Location> fireLineLocations, List<Location> graphFromLocations,
        Map<Location, FrontlineHydrantsBalance> frontlineHydrantsBalance,
        Map<HydrantOutlets, HydrantState> hydrantOutletsState,
        Map<Location, List<HydrantOutlets>> fireLineHydrantOutletsByLocation,
        Map<Location, FirefightingStep> firefightingStep
    ) {
        // Print architecture for verification that we really are under the 32-bit JRE + engine.
        System.out.println("sun.arch.data.model=" + System.getProperty("sun.arch.data.model"));

        // Arrange: use the real shipped topology (not synthetic fixtures) so the test validates
        // behavior of the actual feis.clp rules on the real configuration.

        // deriveThreatenedLocations: fire spreads across BORDERS only (not doors) — every compartment
        // sharing a bulkhead with the fire location. Fully derived from topology.yaml's borders.
        Set<Location> threatenedLocations = FireScenarios.deriveThreatenedLocations(fireLocation);

        // evacuationLocations comes from parameter (for D it is the result of a directed reachability
        // walk over the evacuation-routes graph — materially more involved than simple set-membership).
        Set<Location> evacuationLocations = new LinkedHashSet<>(evacLocations);

        // The "emergency" location set that feis.clp's IMMEDIATE-EXPLOSION / IMMEDIATE-ISOLATION /
        // IMMEDIATE-GERMETISATION rule families key off: (incident fire|threat) OR
        // (evacuation to-evacuate|done) — verified directly against feis.clp:866-874
        // (IMMEDIATE-EXPLOSION::diesel-oil), whose pattern this mirrors.
        Set<Location> emergencyLocations = new LinkedHashSet<>();
        emergencyLocations.add(fireLocation);
        emergencyLocations.addAll(threatenedLocations);
        emergencyLocations.addAll(evacuationLocations);

        Set<Location> explosionThreatLocations = withAttribute(emergencyLocations, location -> location.getExplosiveMaterial().isPresent());
        Set<Location> flammableLocations = withAttribute(emergencyLocations, location -> location.getBurningMaterial().isPresent());
        Set<Location> machineryDamageLocations = withAttribute(emergencyLocations, Location::hasMachinery);
        // IMMEDIATE-GERMETISATION::turn-off-aggregates (feis.clp:773) actually requires
        // (evacuation to-evacuate) specifically, not to-evacuate|done — a distinction this derivation
        // does not reproduce. For scenario D every evacuation-status location that also carries a
        // ventilation system happens to satisfy the stricter condition too, so the broader
        // emergencyLocations union still yields the correct result here.
        Set<Location> ventilationOffLocations = withAttribute(emergencyLocations, location -> location.getVentilationType().isPresent());

        // sealingDoorsToClose: IMMEDIATE-GERMETISATION::close-doors (feis.clp:817-855) closes every
        // open DOOR (not BORDER — a different CLIPS instance type) with at least one endpoint in the
        // emergency set, excluding exits to another deck ("out").
        List<Link> sealingCandidates = FireScenarios.deriveDoorsToCloseCandidates(emergencyLocations);
        // sealingDoorsKeepOpen comes from parameter (HYDRANTS-ALLOCATION::keep-open-doors reopens
        // doors needed for hose routing — dynamic, taken from golden).
        List<Link> sealingDoorsToClose = new ArrayList<>(sealingCandidates);
        sealingDoorsToClose.removeAll(keepOpenDoors);

        // Act
        FireIncidentSnapshot snapshot = service.reportFireIncident(fireLocation);

        // Assert: derived expectations
        assertThat(snapshot.fireLocations(), containsInAnyOrder(fireLocation));
        assertThat(snapshot.threatenedLocations(), is(threatenedLocations));
        assertThat(snapshot.evacuationLocations(), is(evacuationLocations));
        assertThat(snapshot.ventilationOffLocations(), is(ventilationOffLocations));
        assertThat(snapshot.sealingDoorsToClose(), containsInAnyOrder(toArray(sealingDoorsToClose)));
        assertThat(snapshot.sealingDoorsKeepOpen(), containsInAnyOrder(toArray(keepOpenDoors)));
        assertThat(snapshot.explosionThreatLocations(), is(explosionThreatLocations));
        assertThat(snapshot.flammableLocations(), is(flammableLocations));
        assertThat(snapshot.machineryDamageLocations(), is(machineryDamageLocations));

        // Assert: not derived — depend on the CLIPS hose-reach graph and greedy hydrant allocation,
        // which would have to be independently reimplemented in Java to compute. Known values,
        // matching the golden for this scenario.
        assertThat(snapshot.fireLineLinks(), containsInAnyOrder(toArray(fireLineLinks)));

        assertThat(snapshot.fireLineLocations(), is(new LinkedHashSet<>(fireLineLocations)));
        assertThat(snapshot.graphFromLocations(), is(new LinkedHashSet<>(graphFromLocations)));
        assertThat(snapshot.firefightingPlanSteps(), is(firefightingStep));
        assertThat(snapshot.frontlineHydrantsBalance(), is(frontlineHydrantsBalance));
        assertThat(snapshot.hydrantOutletsState(), is(hydrantOutletsState));
        assertThat(snapshot.fireLineHydrantOutletsByLocation(), is(fireLineHydrantOutletsByLocation));
    }

    private static Link[] toArray(List<Link> links) {
        return links.toArray(new Link[0]);
    }

    private static Set<Location> withAttribute(Set<Location> candidates, Predicate<Location> hasAttribute) {
        Set<Location> matching = new LinkedHashSet<>();
        for (Location location : candidates) {
            if (hasAttribute.test(location))
                matching.add(location);
        }
        return matching;
    }

    // ==================== getExplanationFor* (the CLIPS-adjacent "why" popup gap) ====================
    // gui.solution.SolutionResultsControllerTest's resolveExplanationForRow tests cover the other half
    // of this gap: the pure-Java routing that picks which of these six ExpertSystemService methods to
    // call for a given actions-table row. These delegate straight into ClipsEngineAccess's
    // executeQueryTrimmed-based explanation queries, so the real engine is the only way to verify they
    // resolve an actual EXPLAIN instance — see the class javadoc for why this coverage lives here
    // rather than in its own class. Each test reports fire at scenario A itself (service is already
    // reset by @BeforeEach) rather than relying on
    // reportFireIncident_BuildsTheExpectedSnapshotForEachScenario having run first, since
    // parameterized-test method execution order is not guaranteed.
    //
    // Expected antecedent/consequent text is copied verbatim from the feis.clp rule that creates each
    // EXPLAIN instance, not re-derived — matching ClipsEngineAccessTest's "golden/format-string"
    // precedent (see src/test/java/AGENTS.md) — so a wording change in the rule base is caught here
    // instead of silently reaching the operator's popup unnoticed:
    //   - evacuation: IMMEDIATE-EVACUATION::search-initial/::search-move (feis.clp:718, :747) —
    //     location b, cut off by fire at a.
    //   - location/ventilation sealing: IMMEDIATE-GERMETISATION::turn-off-aggregates (feis.clp:789) —
    //     location e, threatened (borders the fire at a via border AE) with an active ventilation system.
    //   - door sealing: IMMEDIATE-GERMETISATION::close-doors (feis.clp:838) — door AQ, directly
    //     adjacent to the fire location a.
    //   - explosion prevention: IMMEDIATE-EXPLOSION::diesel-oil (feis.clp:886) — location e
    //     (explosive diesel_oil, threatened).
    //   - flammable-material prevention: IMMEDIATE-ISOLATION::burning-machine-oil (feis.clp:985) —
    //     location e (burning machine_oil, threatened).
    //   - machinery-damage prevention: IMMEDIATE-ISOLATION::stop-machinery-emergency (feis.clp:1040) —
    //     location e (machinery on, threatened).
    // Location e carries all four hazard attributes at once (topology.yaml), so one threatened
    // location covers the location-sealing, explosion, flammable, and machinery cases.

    private static Stream<ExplanationCase> getExplanationFor_ReturnsTheMatchingRuleExplanation() {
        return Stream.of(
            new ExplanationCase("evacuation",
                service -> service.getExplanationForEvacuation(FireScenarios.TOPOLOGY.location("b")),
                new Explanation(
                    "Evacuation path is cut by fire in a",
                    "Location b haven't been evacuated",
                    "Evacuate location b")),
            new ExplanationCase("location/ventilation sealing",
                service -> service.getExplanationForLocation(FireScenarios.TOPOLOGY.location("e")),
                new Explanation(
                    "Location e is in threat",
                    "Ventilation in e is on",
                    "Turn-off ventilation aggregate in e")),
            new ExplanationCase("door sealing",
                service -> service.getExplanationForDoorSealing(FireScenarios.TOPOLOGY.link("AQ")),
                new Explanation(
                    "Locations a and q are in emergency",
                    "Door is open",
                    "Close door from a to q")),
            new ExplanationCase("explosion prevention",
                service -> service.getExplanationForExplosions(FireScenarios.TOPOLOGY.location("e")),
                new Explanation(
                    "Location e is in emergency",
                    "Possibility of diesel oil explosion",
                    "Pump out diesel oil in e")),
            new ExplanationCase("flammable-material prevention",
                service -> service.getExplanationForFlammable(FireScenarios.TOPOLOGY.location("e")),
                new Explanation(
                    "Location e is in emergency",
                    "Availability of combustible matherial: machine oil",
                    "Pump out machine oil from location e to prevent its ignition")),
            new ExplanationCase("machinery-damage prevention",
                service -> service.getExplanationForMachineryDamage(FireScenarios.TOPOLOGY.location("e")),
                new Explanation(
                    "Location e is in emergency",
                    "Machinery in location e is working",
                    "Stop machinery in location e to prevent ignition"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void getExplanationFor_ReturnsTheMatchingRuleExplanation(ExplanationCase testCase) {
        service.reportFireIncident(FireScenarios.TOPOLOGY.location("a"));

        Explanation explanation = testCase.query().apply(service);

        assertThat(explanation, is(testCase.expected()));
    }

    /**
     * Every {@code getExplanationFor*} method shares this identical null-guard (see
     * {@link ExpertSystemService}) — exercised once here, through one representative method, rather
     * than repeated per method in {@link #getExplanationFor_ReturnsTheMatchingRuleExplanation}.
     */
    @Test
    void getExplanationForEvacuation_NullLocationReturnsEmpty() {
        service.reportFireIncident(FireScenarios.TOPOLOGY.location("a"));

        assertThat(service.getExplanationForEvacuation(null), is(Explanation.EMPTY));
    }

    private record ExplanationCase(String rule, Function<ExpertSystemService, Explanation> query, Explanation expected) {
        @Override
        public String toString() {
            return rule;
        }
    }
}
