package clips;

import config.specification.basic.BorderSpec;
import config.specification.basic.DoorSpec;
import domain.*;
import domain.registry.TopologyModel;
import config.loading.DeckMapTopologyConfig;
import org.junit.jupiter.params.provider.Arguments;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Centralized reference data for the six fire scenarios (A, D, G, J, P, T).
 * <p>
 * This is the single source of truth for post-fire-incident state used by
 * report*Changes tests. Data is derived from the golden masters under
 * src/testClips/resources/clips/diagnostics/golden/.
 * <p>
 * "Thick" record containing the key sets that drive evacuation, door sealing,
 * ventilation, explosion, flammable and machinery prevention, plus helpers
 * to derive targets for composite tests and extinguishers.
 */
public final class FireScenarios {
    /**
     * {@code @ResourceLock} value shared by every test class that touches the native CLIPS engine
     * (whether through a fresh {@code ExpertSystemService} or a reused, reset-between-scenarios
     * one) — see {@link clips.ExpertSystemService#resetForNewScenario()} for why constructing or
     * using more than one native {@code Environment} at a time in one process is unsafe. Declaring
     * this lock ensures JUnit's parallel scheduler (if ever enabled — it is not, today) can never
     * run two of these classes concurrently.
     */
    public static final String CLIPS_ENGINE_RESOURCE = "clipsjni-native-engine";

    public static final DeckMapTopologyConfig CONFIG;
    public static final TopologyModel TOPOLOGY;

    static {
        CONFIG = DeckMapTopologyConfig.createDefault();
        TOPOLOGY = CONFIG.buildTopologyModel();
    }

    public record Scenario(
            Location fireLocation,
            List<Location> evac,
            List<Link> keepOpen,
            List<Link> sealingToClose,
            List<Location> ventOff,
            List<Location> explosion,
            List<Location> flammable,
            List<Location> machinery,
            List<Link> fireLineLinkCodes,
            List<Location> fireLineLocCodes,
            List<Location> graphFromCodes,
            Map<Location, FrontlineHydrantsBalance> frontlineHydrantsBalance,
            Map<HydrantOutlets, HydrantState> hydrantOutletsState,
            Map<Location, List<HydrantOutlets>> fireLineHydrantOutletsByLocation,
            Map<Location, FirefightingStep> firefightingStep
    ) {
        /**
         * Extinguishers whose locations are involved in this scenario.
         */
        public List<Extinguisher> extinguishers() {
            Set<String> involved = new LinkedHashSet<>();
            involved.addAll(evac.stream().map(Location::getCode).toList());
            involved.addAll(ventOff.stream().map(Location::getCode).toList());
            involved.addAll(explosion.stream().map(Location::getCode).toList());
            involved.addAll(flammable.stream().map(Location::getCode).toList());
            involved.addAll(machinery.stream().map(Location::getCode).toList());
            involved.add(fireLocation.getCode());

            return involved.stream()
                    .flatMap(loc -> EXTINGUISHERS_BY_LOCATION.getOrDefault(loc, List.of()).stream())
                    .distinct()
                    .sorted(Comparator.comparing(Extinguisher::getTitle))
                    .toList();
        }
    }

    /** Derived from the real shipped topology, not hand-typed — stays in sync with topology.yaml/groups.yaml. */
    private static final Map<String, List<Extinguisher>> EXTINGUISHERS_BY_LOCATION = TOPOLOGY.allExtinguishers().stream()
            .collect(Collectors.groupingBy(extinguisher -> extinguisher.getLocation().getCode()));

    private static Map<Location, FrontlineHydrantsBalance> parseFrontline(List<String> specs) {
        Map<Location, FrontlineHydrantsBalance> map = new LinkedHashMap<>();
        for (String spec : specs) {
            if (spec != null && !spec.isEmpty()) {
                String[] p = spec.split(":");
                map.put(TOPOLOGY.location(p[0]), new FrontlineHydrantsBalance(Integer.parseInt(p[1]), Integer.parseInt(p[2])));
            }
        }
        return map;
    }

    private static Map<HydrantOutlets, HydrantState> parseHydrantStates(List<String> specs) {
        Map<HydrantOutlets, HydrantState> map = new LinkedHashMap<>();
        for (String spec : specs) {
            if (spec != null && !spec.isEmpty()) {
                String[] p = spec.split(":");
                HydrantOutlets outlet = TOPOLOGY.hydrantOutlets(p[0]);
                map.put(outlet, new HydrantState(outlet, Integer.parseInt(p[1]), Integer.parseInt(p[2])));
            }
        }
        return map;
    }

    private static Map<Location, List<HydrantOutlets>> parseFireLineHydrants(List<String> specs) {
        Map<Location, List<HydrantOutlets>> map = new LinkedHashMap<>();
        for (String spec : specs) {
            if (spec != null && !spec.isEmpty()) {
                String[] p = spec.split(":", 2);
                String locCode = p[0];
                String titlesPart = (p.length > 1 ? p[1] : "");
                List<String> titles = titlesPart.isEmpty() ? List.of() : Stream.of(titlesPart.split(",")).toList();
                List<HydrantOutlets> outs = titles.stream()
                        .filter(t -> !t.isEmpty())
                        .map(TOPOLOGY::hydrantOutlets)
                        .toList();
                map.put(TOPOLOGY.location(locCode), outs);
            }
        }
        return map;
    }

    public static ExpertSystemService createNewExpertSystemService() {
        return new ExpertSystemService(TOPOLOGY);
    }

    public static final List<Scenario> ALL = List.of(
            new Scenario(
                    TOPOLOGY.location("A"),
                    List.of(TOPOLOGY.location("b")),
                    List.of(TOPOLOGY.link("DF"), TOPOLOGY.link("EF")),
                    List.of(TOPOLOGY.link("AQ"), TOPOLOGY.link("AB"), TOPOLOGY.link("DE"), TOPOLOGY.link("DQ")),
                    List.of(TOPOLOGY.location("a"), TOPOLOGY.location("b"), TOPOLOGY.location("e"), TOPOLOGY.location("f")),
                    List.of(TOPOLOGY.location("e"), TOPOLOGY.location("a")),
                    List.of(TOPOLOGY.location("e")),
                    List.of(TOPOLOGY.location("e")),
                    List.of(TOPOLOGY.link("AE"), TOPOLOGY.link("AF"), TOPOLOGY.link("AQ")),
                    List.of(TOPOLOGY.location("q"), TOPOLOGY.location("e"), TOPOLOGY.location("f")),
                    List.of(TOPOLOGY.location("q")),
                    parseFrontline(List.of("q:1:0", "e:1:0", "f:1:0")),
                    parseHydrantStates(List.of("hydr_d1:2:1", "hydr_d2:2:2", "hydr_f:1:0", "hydr_j:3:3", "hydr_m:2:2", "hydr_p:2:2", "hydr_q:1:0")),
                    parseFireLineHydrants(List.of("q:hydr_q", "e:hydr_d1", "f:hydr_f")),
                    buildFirefightingStep(TOPOLOGY.location("a"), TOPOLOGY.location("q"), 1)
            ),
            new Scenario(
                    TOPOLOGY.location("D"),
                    List.of(TOPOLOGY.location("a"), TOPOLOGY.location("b"), TOPOLOGY.location("e"), TOPOLOGY.location("f"), TOPOLOGY.location("q")),
                    List.of(TOPOLOGY.link("HM"), TOPOLOGY.link("HN")),
                    List.of(TOPOLOGY.link("AQ"), TOPOLOGY.link("AB"), TOPOLOGY.link("DE"), TOPOLOGY.link("DF"), TOPOLOGY.link("DH"), TOPOLOGY.link("DJ"), TOPOLOGY.link("DQ"), TOPOLOGY.link("DR"), TOPOLOGY.link("DT"), TOPOLOGY.link("EF"), TOPOLOGY.link("GH"), TOPOLOGY.link("HI"), TOPOLOGY.link("HK"), TOPOLOGY.link("MO")),
                    List.of(TOPOLOGY.location("a"), TOPOLOGY.location("b"), TOPOLOGY.location("e"), TOPOLOGY.location("f"), TOPOLOGY.location("j"), TOPOLOGY.location("o")),
                    List.of(TOPOLOGY.location("e"), TOPOLOGY.location("a"), TOPOLOGY.location("j")),
                    List.of(TOPOLOGY.location("e"), TOPOLOGY.location("j")),
                    List.of(TOPOLOGY.location("e"), TOPOLOGY.location("j")),
                    List.of(TOPOLOGY.link("DH"), TOPOLOGY.link("DJ"), TOPOLOGY.link("DI"), TOPOLOGY.link("DN"), TOPOLOGY.link("DO")),
                    List.of(TOPOLOGY.location("i"), TOPOLOGY.location("o"), TOPOLOGY.location("j"), TOPOLOGY.location("h"), TOPOLOGY.location("n")),
                    List.of(TOPOLOGY.location("h"), TOPOLOGY.location("j")),
                    parseFrontline(List.of("i:0:1", "o:0:1", "j:3:0", "h:1:0", "n:1:0")),
                    parseHydrantStates(List.of("hydr_d1:2:2", "hydr_d2:2:2", "hydr_f:1:1", "hydr_j:3:0", "hydr_m:2:0", "hydr_p:2:2", "hydr_q:1:1")),
                    parseFireLineHydrants(List.of("i:", "o:", "j:hydr_j,hydr_j,hydr_j", "h:hydr_m", "n:hydr_m")),
                    buildFirefightingStep(TOPOLOGY.location("d"), TOPOLOGY.location("j"), 1)
            ),
            new Scenario(
                    TOPOLOGY.location("G"),
                    List.of(),
                    List.of(TOPOLOGY.link("DH"), TOPOLOGY.link("HI"), TOPOLOGY.link("HK"), TOPOLOGY.link("HM")),
                    List.of(TOPOLOGY.link("DF"), TOPOLOGY.link("EF"), TOPOLOGY.link("GH"), TOPOLOGY.link("HN")),
                    List.of(TOPOLOGY.location("f")),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(TOPOLOGY.link("FG"), TOPOLOGY.link("GH"), TOPOLOGY.link("GI"), TOPOLOGY.link("GK")),
                    List.of(TOPOLOGY.location("h"), TOPOLOGY.location("i"), TOPOLOGY.location("k"), TOPOLOGY.location("f")),
                    List.of(TOPOLOGY.location("h")),
                    parseFrontline(List.of("h:1:0", "i:1:0", "k:1:0", "f:1:0")),
                    parseHydrantStates(List.of("hydr_d1:2:2", "hydr_d2:2:1", "hydr_f:1:0", "hydr_j:3:3", "hydr_m:2:0", "hydr_p:2:2", "hydr_q:1:1")),
                    parseFireLineHydrants(List.of("h:hydr_d2", "i:hydr_m", "k:hydr_m", "f:hydr_f")),
                    buildFirefightingStep(TOPOLOGY.location("g"), TOPOLOGY.location("h"), 1)
            ),
            new Scenario(
                    TOPOLOGY.location("J"),
                    List.of(),
                    List.of(),
                    List.of(TOPOLOGY.link("DE"), TOPOLOGY.link("DF"), TOPOLOGY.link("DH"), TOPOLOGY.link("DJ"), TOPOLOGY.link("DQ"), TOPOLOGY.link("DR"), TOPOLOGY.link("DT")),
                    List.of(TOPOLOGY.location("j")),
                    List.of(TOPOLOGY.location("j")),
                    List.of(TOPOLOGY.location("j")),
                    List.of(TOPOLOGY.location("j")),
                    List.of(TOPOLOGY.link("DJ")),
                    List.of(TOPOLOGY.location("d")),
                    List.of(TOPOLOGY.location("d")),
                    parseFrontline(List.of("d:3:0")),
                    parseHydrantStates(List.of("hydr_d1:2:0", "hydr_d2:2:1", "hydr_f:1:1", "hydr_j:3:3", "hydr_m:2:2", "hydr_p:2:2", "hydr_q:1:1")),
                    parseFireLineHydrants(List.of("d:hydr_d2,hydr_d1,hydr_d1")),
                    buildFirefightingStep(TOPOLOGY.location("j"), TOPOLOGY.location("d"), 1)
            ),
            new Scenario(
                    TOPOLOGY.location("P"),
                    List.of(TOPOLOGY.location("c")),
                    List.of(TOPOLOGY.link("MO")),
                    List.of(TOPOLOGY.link("CP"), TOPOLOGY.link("HM"), TOPOLOGY.link("LM"), TOPOLOGY.link("PT")),
                    List.of(TOPOLOGY.location("c"), TOPOLOGY.location("m"), TOPOLOGY.location("o")),
                    List.of(),
                    List.of(TOPOLOGY.location("m")),
                    List.of(),
                    List.of(TOPOLOGY.link("MP"), TOPOLOGY.link("OP")),
                    List.of(TOPOLOGY.location("m"), TOPOLOGY.location("o")),
                    List.of(),
                    parseFrontline(List.of("m:1:0", "o:1:0")),
                    parseHydrantStates(List.of("hydr_d1:2:2", "hydr_d2:2:2", "hydr_f:1:1", "hydr_j:3:3", "hydr_m:2:0", "hydr_p:2:2", "hydr_q:1:1")),
                    parseFireLineHydrants(List.of("m:hydr_m", "o:hydr_m")),
                    buildFirefightingStep(null, null, 0)
            ),
            new Scenario(
                    TOPOLOGY.location("T"),
                    List.of(TOPOLOGY.location("a"), TOPOLOGY.location("b"), TOPOLOGY.location("d"), TOPOLOGY.location("e"), TOPOLOGY.location("f"), TOPOLOGY.location("q")),
                    List.of(),
                    List.of(TOPOLOGY.link("AQ"), TOPOLOGY.link("AB"), TOPOLOGY.link("DE"), TOPOLOGY.link("DF"), TOPOLOGY.link("DH"), TOPOLOGY.link("DJ"), TOPOLOGY.link("DQ"), TOPOLOGY.link("DR"), TOPOLOGY.link("DT"), TOPOLOGY.link("EF"), TOPOLOGY.link("PT")),
                    List.of(TOPOLOGY.location("a"), TOPOLOGY.location("b"), TOPOLOGY.location("e"), TOPOLOGY.location("f")),
                    List.of(TOPOLOGY.location("e"), TOPOLOGY.location("a")),
                    List.of(TOPOLOGY.location("e")),
                    List.of(TOPOLOGY.location("e")),
                    List.of(),
                    List.of(),
                    List.of(),
                    parseFrontline(List.of()),
                    parseHydrantStates(List.of("hydr_d1:2:2", "hydr_d2:2:2", "hydr_f:1:1", "hydr_j:3:3", "hydr_m:2:2", "hydr_p:2:2", "hydr_q:1:1")),
                    parseFireLineHydrants(List.of()),
                    buildFirefightingStep(null, null, 0)
            )
    );

    /** Every location sharing a bulkhead (border) with {@code fireLocation} — fire spreads across borders only. */
    public static Set<Location> deriveThreatenedLocations(Location fireLocation) {
        Set<Location> threatened = new LinkedHashSet<>();
        for (BorderSpec border : CONFIG.borders()) {
            Link link = TOPOLOGY.link(border.link());
            if (link.getFrom().equals(fireLocation) || link.getTo().equals(fireLocation))
                threatened.add(link.getOtherSide(fireLocation));
        }
        return threatened;
    }

    /** Every non-"out" door with at least one endpoint in {@code emergencyLocations}. */
    public static List<Link> deriveDoorsToCloseCandidates(Set<Location> emergencyLocations) {
        List<Link> candidates = new ArrayList<>();
        for (DoorSpec door : CONFIG.doors()) {
            if (!DoorSpec.EXTERNAL_DECK.equalsIgnoreCase(door.to())) {
                Location from = TOPOLOGY.location(door.from());
                Location to = TOPOLOGY.location(door.to());
                if (emergencyLocations.contains(from) || emergencyLocations.contains(to))
                    candidates.add(TOPOLOGY.link(door.from() + door.to()));
            }
        }
        return candidates;
    }

    private static Map<Location, FirefightingStep> buildFirefightingStep(Location planKey, Location planFrom, int planStep) {
        return planKey == null ? Map.of() : Map.of(planKey, new FirefightingStep(planFrom, planStep));
    }

    /** All fire locations for the scenarios (as domain objects). */
    public static final List<Location> ALL_FIRE_LOCATIONS = ALL.stream().map(Scenario::fireLocation).toList();

    // --- Projections for individual test providers (now using domain types) ---

    public static Stream<Arguments> evacuationScenarios() {
        return ALL.stream().map(s -> Arguments.of(s.fireLocation(), s.evac()));
    }

    public static Stream<Arguments> sealingDoorScenarios() {
        return ALL.stream().map(s -> Arguments.of(s.fireLocation(), s.sealingToClose()));
    }

    /**
     * One row per scenario carrying all four damage-prevention target lists together (rather than
     * one row per category) — so the consuming test can report a fire once per scenario and check
     * ventilation/explosion/machinery/flammable unconditionally against their own explicit lists,
     * instead of constructing a fresh {@code ExpertSystemService} per category per scenario.
     */
    public static Stream<Arguments> damagePreventionScenarios() {
        return ALL.stream().map(s -> Arguments.of(s.fireLocation(), s.ventOff(), s.explosion(), s.flammable(), s.machinery()));
    }

    public static Stream<Arguments> extinguisherScenarios() {
        return ALL.stream().map(s -> Arguments.of(s.fireLocation(), s.extinguishers()));
    }

    /** For IncidentReportFireIntegrationTest using domain types. */
    public static Stream<Arguments> fireScenarios() {
        return ALL.stream().map(s -> Arguments.of(
            s.fireLocation(),
            s.evac(),
            s.keepOpen(),
            s.fireLineLinkCodes(),
            s.fireLineLocCodes(),
            s.graphFromCodes(),
            s.frontlineHydrantsBalance(),
            s.hydrantOutletsState(),
            s.fireLineHydrantOutletsByLocation(),
            s.firefightingStep()
        ));
    }

    private FireScenarios() {
        // utility class
    }
}
