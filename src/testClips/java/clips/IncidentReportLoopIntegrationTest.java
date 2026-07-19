package clips;

import domain.Extinguisher;
import domain.Link;
import domain.Location;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import clips.values.DoorState;
import clips.values.EvacuationStatus;
import clips.values.MachineryDamageAction;
import clips.values.VentilationAction;
import clips.values.internal.ExplosionClipsAction;
import clips.values.internal.ExtinguisherClipsStatus;
import clips.values.internal.FlammablePreventionClipsAction;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests that exercise ClipsReportService's report*Changes methods
 * (evacuation, door-sealing, ventilation, explosion-prevention, machinery,
 * flammable, extinguisher) against the real CLIPSJNI 32-bit engine.
 * <p>
 * The suite exercises the real engine using fire incidents at locations A, D, G, J, P and T.
 * <p>
 * Coverage of domain types exercised through report*Changes actions (and the
 * subsequent read-side collect* state) across the six fire locations:
 * <ul>
 *   <li><b>ExplosiveMaterial</b> (3 values): all covered — CHEMICAL_REAGENT (location A,
 *       activated in explosion sets for fires A/D/T), DIESEL_OIL (E in A/D/T),
 *       COMPRESSED_AIR (J in D/J).</li>
 *   <li><b>ExplosiveType</b> (maps 1:1 from material + DONE marker): all covered —
 *       REAGENT (from CHEMICAL_REAGENT in A/D/T), OIL (from DIESEL_OIL in A/D/T),
 *       AIR (from COMPRESSED_AIR in D/J), DONE (used in every
 *       {@code reportExplosionPreventionChanges(..., DONE)} call).</li>
 *   <li><b>FlammableMaterial</b> (2 values): both covered — MACHINE_OIL (E in A/D/T/J
 *       and J in D/J fires), WORKING_CLOTHES (M in P fire flammable set).</li>
 *   <li><b>VentilationType</b> (3 values): all covered — BASIC (e.g. A and similar
 *       in vent-off sets for A/D/T/P), SMOKE_CONTROL (E and J in A/D/J/T),
 *       TRANSIT (F in A/D, M/O in P, etc. across multiple fires).</li>
 *   <li><b>ExtinguisherType</b> (2 values): both covered — CARBON_DIOXIDE (est_a in A/T,
 *       est_e1 in A/D/T, est_j in J, est_m in P), AIR_FOAM (est_e2 in A/D/T,
 *       est_j* in J).</li>
 * </ul>
 * <p>
 * P is retained as an important boundary case for the following reasons:
 * <ul>
 *   <li>Produces an empty explosion threat set (no locations qualify for explosion
 *       prevention reporting).</li>
 *   <li>Produces an empty machinery damage set.</li>
 *   <li>Exercises the alternative flammable material WORKING_CLOTHES (instead of
 *       MACHINE_OIL).</li>
 *   <li>Provides CARBON_DIOXIDE extinguisher coverage via location M.</li>
 *   <li>Still exercises evacuation, door-sealing, ventilation and extinguisher
 *       reporting paths.</li>
 *   <li>Allows verification of report*Changes behavior when certain action sets
 *       are empty after the initial fire report.</li>
 * </ul>
 * <p>
 * G is a similarly useful boundary case: empty evacuation, empty explosion/flammable/machinery
 * sets and no extinguishers, exercising only door-sealing and ventilation reporting.
 * <p>
 * <b>One shared {@code ExpertSystemService} per class, reset between scenarios</b> — not one per
 * scenario. {@code CLIPSJNI.Environment} has no public dispose API (only a private native {@code
 * destroyEnvironment}, called from its own {@code finalize()}); constructing and destroying many
 * short-lived environments in one JVM process was found to reliably crash the JVM ({@code
 * EXCEPTION_ACCESS_VIOLATION} inside CLIPSJNI.dll, a finalizer race) once this class and {@link
 * IncidentReportFireIntegrationTest} together pushed the total environment count high enough —
 * confirmed both ways experimentally: forcing {@code System.gc()}/{@code System.runFinalization()}
 * between tests made it worse (~75% of runs failed vs. ~25% without, across 8 runs), while
 * switching to one construct-once/reset-between-scenarios environment eliminated the crash
 * entirely across repeated stress runs at equal or higher scenario counts. See {@link
 * ExpertSystemService#resetForNewScenario()} for the mechanism and {@code
 * clips.ClipsEnvironmentLifecycleTest} for the reset/clear semantics this relies on.
 * <p>
 * Because the shared {@link ExpertSystemService} is mutable instance state accessed sequentially
 * across test methods, this class must never run its methods concurrently — {@code
 * @Execution(SAME_THREAD)} pins it regardless of any future global parallel-execution
 * configuration, and {@code @ResourceLock(FireScenarios#CLIPS_ENGINE_RESOURCE)} additionally
 * prevents JUnit from ever scheduling this class concurrently with any other class that touches
 * the native CLIPS engine (there is only ever one native {@code Environment} alive per process
 * today; true concurrent use from two threads is untested and assumed unsafe).
 * <p>
 * Run with: ./gradlew testClips
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = FireScenarios.CLIPS_ENGINE_RESOURCE, mode = ResourceAccessMode.READ_WRITE)
class IncidentReportLoopIntegrationTest {
    private ExpertSystemService service;
    private ClipsReportService reportService;
    private ClipsReadOnlyService readOnlyService;

    @BeforeAll
    void beforeAll() {
        service = FireScenarios.createNewExpertSystemService();
        reportService = service;
        readOnlyService = service;
    }

    @BeforeEach
    void beforeEach() {
        service.resetForNewScenario();
    }

    private static Stream<Arguments> reportEvacuationChanges_RoundTrip() {
        return FireScenarios.evacuationScenarios();
    }

    @ParameterizedTest(name = "fire at {0}")
    @MethodSource
    void reportEvacuationChanges_RoundTrip(Location fireLocation, List<Location> evacuationTargets) {
        // Print to verify we run on the real 32-bit portable JRE + CLIPS engine.
        System.out.println("sun.arch.data.model=" + System.getProperty("sun.arch.data.model"));

        reportService.reportFireIncident(fireLocation);

        for (Location targetLocation : evacuationTargets) {
            // Sanity: target starts in to-evacuate after reportFireIncident for this fire.
            assertThat(readOnlyService.collectEvacuationLocations("to-evacuate"), hasItem(targetLocation));

            // Act: operator reports that evacuation of the target is complete.
            reportService.reportEvacuationChanges(Map.of(targetLocation, EvacuationStatus.DONE));

            // Assert
            assertThat(readOnlyService.collectEvacuationLocations("to-evacuate"), not(hasItem(targetLocation)));
            assertThat(readOnlyService.collectEvacuationLocations("done"), hasItem(targetLocation));
        }
    }

    private static Stream<Arguments> reportDoorSealingChanges_RoundTrip() {
        return FireScenarios.sealingDoorScenarios();
    }

    @ParameterizedTest(name = "fire at {0}")
    @MethodSource
    void reportDoorSealingChanges_RoundTrip(Location fireLocation, List<Link> doorCodes) {
        reportService.reportFireIncident(fireLocation);

        for (Link door : doorCodes) {
            // Sanity: the door starts in to-close after reportFireIncident for this fire.
            // Link code order matches how doors are registered (from+to) and how collect returns them.
            assertThat(readOnlyService.collectSealingDoors("to-close"), hasItem(door));

            // Act: operator reports the door as sealed (closed).
            reportService.reportDoorSealingChanges(Map.of(door, DoorState.CLOSE));

            // Assert: the reported door is removed from the to-close set.
            // (keep-open set is a derived CLIPS status for hose routing; we only ever report CLOSE/OPEN.)
            assertThat(readOnlyService.collectSealingDoors("to-close"), not(hasItem(door)));
        }
    }

    private static Stream<Arguments> damagePreventionActions_RoundTrip() {
        return FireScenarios.damagePreventionScenarios();
    }

    /**
     * One test per scenario (not one per category) — deliberately, to keep this method's own
     * contribution to the shared engine's reset cycles no higher than the other tests in this
     * class: each of the four target lists is its own explicit data from {@link FireScenarios}, so
     * every category is checked unconditionally, with no runtime membership test deciding whether
     * to assert.
     */
    @ParameterizedTest(name = "fire at {0}")
    @MethodSource
    void damagePreventionActions_RoundTrip(Location fireLocation, List<Location> ventilationTargets,
        List<Location> explosionTargets, List<Location> flammableTargets, List<Location> machineryTargets)
    {
        reportService.reportFireIncident(fireLocation);

        for (Location targetLocation : ventilationTargets) {
            // Sanity: target starts in to-off after reportFireIncident for this fire.
            assertThat(readOnlyService.collectSealingLocations("to-off"), hasItem(targetLocation));

            // Act: operator switches ventilation off for the target.
            reportService.reportVentilationChanges(Map.of(targetLocation, VentilationAction.OFF));

            // Assert
            assertThat(readOnlyService.collectSealingLocations("to-off"), not(hasItem(targetLocation)));
        }

        for (Location targetLocation : explosionTargets) {
            // Sanity: target starts in the explosion-threat phase after reportFireIncident for this fire.
            assertThat(readOnlyService.collectActionPhase("explosion"), hasItem(targetLocation));

            // Act: operator reports the explosion-prevention action complete for the target.
            Set<Location> pending = reportService.reportExplosionPreventionChanges(Map.of(targetLocation, ExplosionClipsAction.DONE));

            // Assert
            assertThat(pending, not(hasItem(targetLocation)));
        }

        for (Location targetLocation : machineryTargets) {
            // Sanity: target starts in the to-stop machinery-damage phase after reportFireIncident for this fire.
            assertThat(readOnlyService.collectMachineryDamageLocations("stop"), hasItem(targetLocation));

            // Act: operator reports the machinery-damage-prevention action complete for the target.
            reportService.reportMachineryDamagePreventionChanges(Map.of(targetLocation, MachineryDamageAction.DONE));

            // Assert
            assertThat(readOnlyService.collectMachineryDamageLocations("stop"), not(hasItem(targetLocation)));
        }

        for (Location targetLocation : flammableTargets) {
            // Sanity: target starts in the isolation phase after reportFireIncident for this fire.
            assertThat(readOnlyService.collectActionPhase("isolation"), hasItem(targetLocation));

            // Act: operator reports the flammable-prevention action complete for the target.
            reportService.reportFlammablePreventionChanges(Map.of(targetLocation, FlammablePreventionClipsAction.DONE));

            // Assert
            assertThat(readOnlyService.collectActionPhase("isolation"), not(hasItem(targetLocation)));
        }
    }

    private static Stream<Arguments> reportExtinguisherChanges_RoundTrip() {
        return FireScenarios.extinguisherScenarios();
    }

    @ParameterizedTest(name = "fire at {0}")
    @MethodSource
    void reportExtinguisherChanges_RoundTrip(Location fireLocation, List<Extinguisher> extinguishers) {
        reportService.reportFireIncident(fireLocation);

        for (Extinguisher extinguisher : extinguishers) {
            final Location location = extinguisher.getLocation();
            // Sanity: the extinguisher is available at its location after reportFireIncident.
            assertThat(readOnlyService.getExtinguishersForLocation(location), hasItem(extinguisher));

            // Act
            reportService.reportExtinguisherChanges(Map.of(extinguisher, ExtinguisherClipsStatus.USED));

            // Assert: the reported one is removed.
            assertThat(readOnlyService.getExtinguishersForLocation(location), not(hasItem(extinguisher)));
        }
    }
}
