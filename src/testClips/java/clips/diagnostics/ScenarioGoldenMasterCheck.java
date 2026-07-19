package clips.diagnostics;

import clips.ExpertSystemService;
import clips.FireIncidentSnapshot;
import config.loading.DeckMapTopologyConfig;
import domain.registry.TopologyModel;
import util.Charsets;
import util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Golden-master / characterization check for a full fire scenario end to end: real
 * {@code topology.yaml} + real {@code feis.clp} + real {@link ExpertSystemService}, comparing the
 * resulting {@link FireIncidentSnapshot} against a checked-in recorded baseline. Catches
 * regressions unit tests structurally cannot (they never load the real 32-bit CLIPSJNI engine,
 * see {@code src/test/java/clips/ClipsEngineAccessTest}'s javadoc).
 * <p>
 * Deliberately asserts on the <em>structured</em> {@link FireIncidentSnapshot} the application
 * actually consumes, not on CLIPS's console printout text: the printout is a human-debugging
 * side channel nothing in the app reads back, so diffing it would make this check brittle against
 * changes that do not affect real behavior (and fragile to capture correctly in the first place —
 * CLIPS's native {@code printout t} does not necessarily route through Java's redirectable
 * {@code System.out}).
 * <p>
 * Run via {@code ./gradlew runClipsDiagnostic -PdiagClass=ScenarioGoldenMasterCheck} to compare
 * against the recorded baselines, or with {@code -PdiagArgs=record} to (re)record them after a
 * deliberate behavior change — review the resulting diff under
 * {@code src/testClips/resources/clips/diagnostics/golden/} before committing it.
 */
public class ScenarioGoldenMasterCheck {
    /** One fire location per distinct rule path: post/explosion (A), hub with many neighbors (D),
     *  small uninhabited compartment (G), engine-room with explosion+flammable+machinery+ОХТ (J),
     *  hydrant-outlet-rich (P). R and T are the dead-end, different-tank compartments with no
     *  door-adjacent evacuation-none neighbor — {@code get-plan-from} answers bare {@code FALSE}
     *  for them (no route computed, a real, confirmed scenario, see
     *  {@code ClipsEngineAccess#getStepFrom}'s javadoc), which a prior version of this codebase
     *  mishandled by crashing (naive quote-stripping on the literal "FALSE" string); asserting on
     *  them locks down the {@code FALSE}-to-empty-plan contract. */
    private static final List<String> SCENARIOS = List.of("A", "D", "G", "J", "P", "R", "T");
    private static final Path GOLDEN_DIR = Path.of("src/testClips/resources/clips/diagnostics/golden");

    public static void main(String[] args) throws IOException {
        boolean record = args.length > 0 && "record".equalsIgnoreCase(args[0]);
        DiagnosticReport report = new DiagnosticReport();

        DeckMapTopologyConfig config = DeckMapTopologyConfig.createDefault();
        TopologyModel topology = config.buildTopologyModel();

        for (String fireCode : SCENARIOS) {
            report.section("scenario: fire at " + fireCode);

            // A fresh engine per scenario: reportFireIncident mutates engine state (evacuation,
            // sealing, ACTION facts...), so scenarios must not share one running instance.
            ExpertSystemService service = new ExpertSystemService(topology);
            FireIncidentSnapshot snapshot = service.reportFireIncident(topology.location(fireCode));
            String actual = summarize(fireCode, snapshot);

            if (record) {
                Files.createDirectories(GOLDEN_DIR);
                Files.writeString(goldenSourcePath(fireCode), actual);
                report.check("recorded golden baseline for " + fireCode, true);
            } else {
                String expected = readGoldenResource(fireCode);
                boolean matches = expected.equals(actual);
                report.check("matches recorded baseline for " + fireCode, matches);
                if (!matches) {
                    System.out.println("--- expected (golden) ---");
                    System.out.println(expected);
                    System.out.println("--- actual ---");
                    System.out.println(actual);
                }
            }
        }

        report.finish();
    }

    private static Path goldenSourcePath(String fireCode) {
        return GOLDEN_DIR.resolve("scenario-" + fireCode + ".txt");
    }

    private static String readGoldenResource(String fireCode) throws IOException {
        String resourcePath = "clips/diagnostics/golden/scenario-" + fireCode + ".txt";
        try (InputStream in = ResourceUtil.openResourceStream(resourcePath)) {
            return new String(in.readAllBytes(), Charsets.UTF_8);
        }
    }

    private static String summarize(String fireCode, FireIncidentSnapshot snapshot) {
        StringBuilder text = new StringBuilder();
        text.append("fire: ").append(fireCode).append('\n');
        text.append("fireLocations: ").append(snapshot.fireLocations()).append('\n');
        text.append("threatenedLocations: ").append(snapshot.threatenedLocations()).append('\n');
        text.append("evacuationLocations: ").append(snapshot.evacuationLocations()).append('\n');
        text.append("ventilationOffLocations: ").append(snapshot.ventilationOffLocations()).append('\n');
        text.append("sealingDoorsToClose: ").append(snapshot.sealingDoorsToClose()).append('\n');
        text.append("sealingDoorsKeepOpen: ").append(snapshot.sealingDoorsKeepOpen()).append('\n');
        text.append("explosionThreatLocations: ").append(snapshot.explosionThreatLocations()).append('\n');
        text.append("flammableLocations: ").append(snapshot.flammableLocations()).append('\n');
        text.append("machineryDamageLocations: ").append(snapshot.machineryDamageLocations()).append('\n');
        text.append("fireLineLinks: ").append(snapshot.fireLineLinks()).append('\n');
        text.append("fireLineLocations: ").append(snapshot.fireLineLocations()).append('\n');
        text.append("graphFromLocations: ").append(snapshot.graphFromLocations()).append('\n');
        text.append("frontlineHydrantsBalance: ").append(snapshot.frontlineHydrantsBalance()).append('\n');
        text.append("hydrantStates: ").append(snapshot.hydrantOutletsState()).append('\n');
        text.append("firefightingPlanSteps: ").append(snapshot.firefightingPlanSteps()).append('\n');
        text.append("extBToByLocation: ").append(snapshot.extBToByLocation()).append('\n');
        text.append("extByLocation: ").append(snapshot.extByLocation()).append('\n');
        text.append("extBFromByLocation: ").append(snapshot.extBFromByLocation()).append('\n');
        text.append("fireLineHydrantOutletsByLocation: ").append(snapshot.fireLineHydrantOutletsByLocation()).append('\n');
        return text.toString();
    }
}
