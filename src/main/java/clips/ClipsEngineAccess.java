package clips;

import CLIPSJNI.Environment;
import CLIPSJNI.MultifieldValue;
import CLIPSJNI.PrimitiveValue;
import CLIPSJNI.StringValue;
import domain.Border;
import domain.Door;
import domain.types.FlammableMaterial;
import domain.EvacuationRoute;
import domain.types.ExplosiveMaterial;
import domain.Extinguisher;
import domain.FireHoseSpan;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import util.ResourceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Low-level access layer to the CLIPS expert system engine.
 * <p>
 * This class is responsible for direct interaction with CLIPSJNI.Environment: loading rules, running the inference
 * engine, executing queries and commands, and providing thin, well-documented wrappers around specific CLIPS functions.
 * Every query method returns domain objects (resolved against the {@link TopologyModel} held here), not raw CLIPS
 * strings — the translation in both directions (domain→string for {@link #initializeTopology}, string→domain for every
 * query below) lives here, not in {@link ExpertSystemService}, which is left as pure orchestration (business logic,
 * firefighting-plan assembly, {@link FireIncidentSnapshot} construction) over already-resolved domain types.
 * <p>
 * <b>Environment lifecycle caution:</b> {@code CLIPSJNI.Environment} has no public dispose API — only a private native
 * {@code destroyEnvironment}, invoked from its own {@code finalize()} — so teardown is entirely GC-finalizer-driven,
 * not something a caller can trigger deterministically. Constructing and destroying more than one {@code Environment}
 * in the same JVM process was found to reliably crash the JVM ({@code EXCEPTION_ACCESS_VIOLATION} inside CLIPSJNI.dll,
 * a finalizer race — see {@link ExpertSystemService#resetForNewScenario()} for the full history and the fix that
 * replaced repeated construction with reuse). Practical consequence: at most one {@code Environment} — and so at most
 * one {@code ClipsEngineAccess} — should be alive per JVM process at a time. To run several independent scenarios in
 * one process, construct exactly one instance and call {@link #reset()} between scenarios instead of constructing a new
 * instance each time. Never run two scenarios against the same instance concurrently: the underlying
 * {@code Environment} is not thread-safe, and even otherwise-sequential callers must not be reordered onto multiple
 * threads (see the {@code @Execution}/{@code @ResourceLock} annotations on this class's test callers for how that's
 * enforced there).
 */
public class ClipsEngineAccess {
    private static final String DOOR_NAME_FORMAT = "door_%s_to_%s";
    private static final String BORDER_NAME_FORMAT = "border_%s_upon_%s";
    private static final String EVACUATION_NAME_FORMAT = "evac_%s_to_%s";
    private static final String FIRE_HOSE_SPAN_NAME_FORMAT = "hosespan_%s_%s";

    private final Environment clips;
    private final TopologyModel topology;

    public ClipsEngineAccess(String clipsRulesBase, TopologyModel topology) {
        clips = new Environment();
        clips.load(ResourceUtil.resolveResourcePath(clipsRulesBase));
        clips.reset();
        this.topology = topology;
        System.out.printf("CLIPS initialized: CLIPSJNI: %s, CLIPS: %s%n", Environment.getCLIPSJNIVersion(), Environment.getCLIPSVersion());
    }

    /**
     * Reinitializes CLIPS's fact-list and instance-list for a new scenario — removes every fact and every instance
     * (including the {@code LOCATION}/{@code HYDRANT}/{@code BORDER}/etc. instances {@link #initializeTopology}
     * created) and reasserts {@code initial-fact}, but leaves the loaded rule base (
     * {@code defrule}/{@code deftemplate}/{@code deffunction}/COOL class definitions from {@code feis.clp}) fully
     * intact — confirmed empirically via {@code clips.ClipsEnvironmentLifecycleTest}. This is CLIPS's own
     * {@code (reset)} command, distinct from {@code (clear)} (which also wipes the rule base itself, requiring a fresh
     * {@code load()}).
     * <p>
     * Callers must invoke {@link #initializeTopology} again afterward before running a new scenario, since
     * {@code reset()} removed the topology instances too. See {@link ExpertSystemService#resetForNewScenario()}, the
     * only caller.
     */
    void reset() {
        clips.reset();
    }

    // ==================== TOPOLOGY INITIALIZATION ====================

    /**
     * Creates the topology COOL instances ({@code LOCATION}, {@code HYDRANT}, {@code BORDER}, {@code DOOR}) from the
     * {@link TopologyModel} held by this instance — the single source of truth for the whole topology, including
     * borders / doors (see {@link Border}/{@link Door}) — replacing the (removed) hardcoded {@code definstances}
     * entries for these classes in {@code feis.clp}. Must be called after {@code load()}/{@code reset()} and before any
     * rule-firing interaction for that scenario. {@link ExpertSystemService#resetForNewScenario()} calls
     * {@link #reset()} then this method again so the same {@code Environment} can host another independent scenario —
     * not "once per JVM", once per scenario after reset.
     * <ul>
     * <li> {@code LOCATION}: created fully in one step — identity plus every scenario attribute (area, tank,
     * compartment type, ventilation, explosive / burning material, machinery, chemical-suppression system) — all
     * sourced from {@code topology.yaml}; {@code feis.clp} no longer owns any of this data (see
     * {@link #fromLocationInstance}). </li> <li> {@code HYDRANT}: created fully (identity + location + outlet
     * count).</li> <li> {@code BORDER}: each undirected bulkhead is created in both directions (fire spreads
     * symmetrically), carrying its shared-wall length.</li> <li> {@code DOOR}: every door, including exits to another
     * deck ({@code to = out}); see {@link Location#OUT}. </li> <li> {@code EVACUATION}: each directed escape edge (
     * {@code from → to}); a passage open both ways is two separate routes (identity the app fully owns, seeded from the
     * model).</li> <li> {@code FIRE-DISTANCE}: every door-to-door and hydrant-to-door hose-reach span (identity the app
     * fully owns, seeded from the model).</li> <li> {@code EXTINGUISHER}: every portable device, instance-named by its
     * own title (identity the app fully owns, seeded from the model).</li>
     * </ul>
     */
    void initializeTopology() {
        for (Location location : topology.allLocations())
            makeInstanceOrThrow(fromLocationInstance(location));

        for (HydrantOutlets hydrant : topology.allHydrantOutlets())
            makeInstanceOrThrow(fromHydrantInstance(hydrant));

        for (Extinguisher extinguisher : topology.allExtinguishers())
            makeInstanceOrThrow(fromExtinguisherInstance(extinguisher));

        for (Border border : topology.allBorders()) {
            String from = border.getLink().getFrom().getCode();
            String upon = border.getLink().getTo().getCode();
            makeInstanceOrThrow(fromBorderInstance(from, upon, border.getLength()));
            makeInstanceOrThrow(fromBorderInstance(upon, from, border.getLength()));
        }

        for (Door door : topology.allDoors()) {
            String from = door.getFrom().getCode();
            String to = door.getTo().getCode();
            makeInstanceOrThrow(fromDoorInstance(from, to));
        }

        for (EvacuationRoute route : topology.allEvacuationRoutes())
            makeInstanceOrThrow(fromEvacuationRouteInstance(route.getFrom().getCode(), route.getTo().getCode()));

        for (FireHoseSpan<Link> span : topology.allDoorToDoorFireHoseSpans())
            makeInstanceOrThrow(fromDoorToDoorFireHoseSpanInstance(span.getFrom(), span.getTo(), span.getDistance()));

        for (FireHoseSpan<HydrantOutlets> span : topology.allHydrantToDoorFireHoseSpans())
            makeInstanceOrThrow(fromHydrantToDoorFireHoseSpanInstance(span.getFrom(), span.getTo(), span.getDistance()));
    }

    /**
     * Builds the full {@code LOCATION} instance definition in one step — identity plus every scenario attribute
     * {@code location-attrs}/{@code apply-location-attributes} used to seed separately (now sourced from
     * {@link Location} directly; authors write the same fields under {@code location-labels} in {@code topology.yaml}).
     * {@code explosive}/{@code burning} fall back to CLIPS's own {@code none} default when absent, matching the
     * original data exactly. {@code ventil} is only {@code on}/{@code none} from whether a ventilation system is
     * present — the concrete {@link domain.types.VentilationType} never reaches CLIPS.
     */
    static String fromLocationInstance(Location location) {
        String code = location.getCode();
        String explosive = location.getExplosiveMaterial().map(ExplosiveMaterial::getClipsValue).orElse("none");
        String burning = location.getBurningMaterial().map(FlammableMaterial::getClipsValue).orElse("none");
        String ventilation = location.getVentilationType().isPresent() ? "on" : "none";
        String machinery = location.hasMachinery() ? "on" : "none";
        String chemicalSuppression = location.hasChemicalSuppression() ? "yes" : "none";
        return String.format("(%s of LOCATION (title %s) (area %s) (tank %s) (type %s) (ventil %s)"
                + " (explosive %s) (burning %s) (machinery %s) (co %s))",
            code, code, location.getArea(), location.getTank(), location.getType().getClipsValue(),
            ventilation, explosive, burning, machinery, chemicalSuppression);
    }

    static String fromHydrantInstance(HydrantOutlets hydrant) {
        String title = hydrant.getTitle();
        int outlets = hydrant.getOutlets();
        return String.format("(%s of HYDRANT (title %s) (location %s) (number %s) (free %s))",
            title, title, hydrant.getLocation().getCode(), outlets, outlets);
    }

    /** Instance-named by its own title (like {@code HYDRANT}); always seeded {@code (used no)}. */
    static String fromExtinguisherInstance(Extinguisher extinguisher) {
        String title = extinguisher.getTitle();
        return String.format("(%s of EXTINGUISHER (title %s) (location %s) (type %s) (used no))",
            title, title, extinguisher.getLocation().getCode(), extinguisher.getType().getClipsValue());
    }

    static String fromBorderInstance(String fromCode, String uponCode, double length) {
        String name = String.format(BORDER_NAME_FORMAT, fromCode, uponCode);
        return String.format("(%s of BORDER (from %s) (upon %s) (length %s))", name, fromCode, uponCode, length);
    }

    static String fromDoorInstance(String fromCode, String toCode) {
        String name = doorInstanceName(fromCode, toCode);
        return String.format("(%s of DOOR (from %s) (to %s))", name, fromCode, toCode);
    }

    static String fromEvacuationRouteInstance(String fromCode, String toCode) {
        String name = String.format(EVACUATION_NAME_FORMAT, fromCode, toCode);
        return String.format("(%s of EVACUATION (from %s) (to %s))", name, fromCode, toCode);
    }

    /**
     * A door-to-door hose-reach span: {@code from}/{@code to} are each a door, represented in CLIPS as the two-token
     * {@code (from <locA> <locB>)} multislot form the rule base's {@code arrange-letters} -based matching expects (see
     * {@code clips/README.md}).
     */
    static String fromDoorToDoorFireHoseSpanInstance(Link from, Link to, double distance) {
        String name = String.format(FIRE_HOSE_SPAN_NAME_FORMAT, from.getCode().toLowerCase(), to.getCode().toLowerCase());
        return String.format("(%s of FIRE-DISTANCE (from %s %s) (to %s %s) (value %s))",
            name, from.getFrom().getCode(), from.getTo().getCode(), to.getFrom().getCode(), to.getTo().getCode(), distance);
    }

    /**
     * A hydrant-to-door hose-reach span: {@code from} is a single hydrant title token, {@code to} is a door in the same
     * two-token multislot form as {@link #fromDoorToDoorFireHoseSpanInstance}.
     */
    static String fromHydrantToDoorFireHoseSpanInstance(HydrantOutlets from, Link to, double distance) {
        String name = String.format(FIRE_HOSE_SPAN_NAME_FORMAT, from.getTitle(), to.getCode().toLowerCase());
        return String.format("(%s of FIRE-DISTANCE (from %s) (to %s %s) (value %s))",
            name, from.getTitle(), to.getFrom().getCode(), to.getTo().getCode(), distance);
    }

    /**
     * The CLIPS instance name the application must use to address a door — the same name {@link #fromDoorInstance}
     * creates it under. This naming convention is the Java↔CLIPS mapping contract; CLIPS itself matches doors by
     * {@code from}/{@code to} slot values, so the string is only ever built here, never parsed back.
     */
    static String doorInstanceName(String fromCode, String toCode) {
        return String.format(DOOR_NAME_FORMAT, fromCode, toCode);
    }

    private void makeInstanceOrThrow(String instanceSpec) {
        if (clips.makeInstance(instanceSpec) == null)
            throw new IllegalStateException("CLIPS failed to create instance: " + instanceSpec);
    }

    // ==================== RESPONSE PARSING (raw CLIPS strings → domain objects) ====================
    // static and taking TopologyModel explicitly (rather than instance methods closing over the
    // field) so this pure string-to-domain conversion is directly unit testable on a normal JVM,
    // without the real 32-bit CLIPS engine — see ClipsEngineAccessParsingTest. Every query method
    // below feeds these from the field, so a caller never sees the intermediate raw string.

    /**
     * Parses a concatenated string of single-character location codes (as returned by the CLIPS
     * {@code collect-*}/{@code get-*-locations} family of queries) into resolved {@link Location} values. Returns an
     * empty set for an empty input string.
     */
    static Set<Location> parseLocations(TopologyModel topology, String concatenated) {
        Set<Location> locations = new LinkedHashSet<>();
        for (int i = 1; i <= concatenated.length(); i++)
            locations.add(topology.location(concatenated.substring(i - 1, i)));
        return locations;
    }

    /**
     * Parses a space-separated string of hydrant titles (as returned by the CLIPS
     * {@code get-fire-line-hydr}/{@code get-ext-*-for-location} family of queries) into validated
     * {@link HydrantOutlets} values, skipping blank tokens. Returns an empty list for {@code null} or blank input.
     */
    static List<HydrantOutlets> parseHydrantOutlets(TopologyModel topology, String data) {
        if (data == null || data.isBlank())
            return List.of();

        return Arrays.stream(data.split(" "))
                .filter(rawTitle -> !rawTitle.isBlank())
                .map(topology::hydrantOutlets)
                .toList();
    }

    /**
     * Parses a space-separated string of extinguisher titles (as returned by {@code get-extinguishers-for-location})
     * into validated {@link Extinguisher} values, skipping blank tokens. Returns an empty list for {@code null} or
     * blank input.
     */
    static List<Extinguisher> parseExtinguishers(TopologyModel topology, String data) {
        if (data == null || data.isBlank())
            return List.of();

        return Arrays.stream(data.split(" "))
                .filter(rawTitle -> !rawTitle.isBlank())
                .map(topology::extinguisher)
                .toList();
    }

    /**
     * Parses a concatenated string of two-character door / link codes (as returned by the CLIPS
     * {@code collect-germ-door}/{@code get-line1-borders} family of queries) into resolved {@link Link} values. Returns
     * an empty list for an empty input string.
     * <p>
     * An odd-length input is rejected rather than silently truncated. This is currently unreachable from either real
     * caller: {@code get-line1-borders} only ever walks {@code BORDER} instances, whose endpoints are always real
     * locations, and {@code collect-germ-door} is only ever queried for the {@code to-close}/{@code keep-open}
     * statuses, both of which the rule base only assigns to interior doors (see
     * {@code IMMEDIATE-GERMETISATION::close-doors}'s {@code ~out} guard and
     * {@code HYDRANTS-ALLOCATION::keep-open-doors}'s single-character path-substring pairing) — a door to
     * {@link Location#OUT} never reaches either status. Guarded here anyway so a future rule-base change that breaks
     * that invariant fails loudly instead of silently dropping the trailing code.
     */
    static List<Link> parseLocationLinks(TopologyModel topology, String concatenated) {
        if (concatenated.length() % 2 != 0)
            throw new IllegalStateException("Malformed door/link code string (odd length): '" + concatenated + "'");

        List<Link> links = new ArrayList<>();
        for (int i = 0; i < concatenated.length() / 2; i++)
            links.add(topology.link(concatenated.substring(i * 2, i * 2 + 2)));
        return links;
    }

    // ==================== LOW-LEVEL QUERY / COMMAND HELPERS ====================

    /**
     * The literal string CLIPS's {@code eval} returns when a command or query could not be resolved / dispatched — an
     * unquoted {@code FALSE} symbol (see {@code SymbolValue}), never mistakable for a legitimate quoted-string or
     * integer response. Verified empirically: a {@code send} to a nonexistent instance returns this reliably, even
     * though CLIPS's console only ever prints the underlying {@code [MSGPASS2]} error once per engine session — the
     * return value is the only trustworthy signal, the printed message is not.
     */
    private static final String CLIPS_FALSE = "FALSE";

    void executeIncident(Location locationCode, String incident) {
        evalOrThrow(sendMessage(locationCode.getCode(), "put-accedent", incident));
        clips.run();
    }

    <T> T executeWithFocus(Supplier<T> runnable, String focus) {
        T result = runnable.get();
        clips.eval("(focus MAIN)");
        clips.eval("(focus " + focus + ")");
        clips.run();
        return result;
    }

    /**
     * Evaluates a fire-and-forget CLIPS command (a {@code send ... put-<slot>} report) and fails fast if it returns
     * {@link #CLIPS_FALSE}. Safe for every {@code put-<slot>} default message handler used by this class: on success
     * they echo back the value just set (e.g. {@code "close "}, {@code "on "}, {@code "stop "}) — none of this app's
     * slot vocabularies collides with the literal {@code FALSE}, so a {@code FALSE} result unambiguously means the
     * {@code send} could not be dispatched (almost always a bad instance address).
     * <p>
     * Not used for {@code action-edit} (see {@link #reportFlammablePrevention}/{@link #reportExplosionPrevention}):
     * that deffunction also answers {@code FALSE} when there is simply no matching {@code ACTION} fact to update — a
     * legitimate, common outcome — so its {@code FALSE} is not a reliable failure signal and is deliberately left
     * unchecked here.
     */
    private void evalOrThrow(String operation) {
        String result = clips.eval(operation).toString();
        if (CLIPS_FALSE.equals(result))
            throw new IllegalStateException("CLIPS command failed (returned FALSE): " + operation);
    }

    /**
     * Builds a {@code (send [instance] message-name args)} expression — the CLIPS message-passing syntax every
     * {@code evalOrThrow} call below sends. Plain concatenation, not {@code String.format}: {@code args} (and
     * occasionally {@code targetInstance}) are values from other layers, not literals under our control, and a stray
     * {@code %} in either would corrupt or throw with a format string where concatenation just can't.
     */
    private static String sendMessage(String targetInstance, String messageName, String args) {
        return "(send [" + targetInstance + "] " + messageName + " " + args + ")";
    }

    /**
     * Evaluates a query whose successful result is either a CLIPS string (built via {@code bind ?collection ""} +
     * {@code str-cat}) or a raw multislot value returned directly (e.g. {@code hydrants-titles},
     * {@code antec1}/{@code antec2}/{@code consec}) — CLIPSJNI represents these as a {@link StringValue} or a
     * {@link MultifieldValue} respectively, never as plain Java text, so the actual runtime type is checked rather than
     * guessed from {@code toString()} content (a multifield's {@code toString()}, e.g. {@code "(hydr_f hydr_j)"} or
     * {@code "()"} when empty, is not distinguishable from other unexpected shapes by string inspection alone). A
     * multifield's elements are joined with a single space, matching every caller's existing "space-separated titles"
     * contract. Fails fast for anything else — in particular the bare, unquoted {@link #CLIPS_FALSE} symbol an
     * unresolved / misdispatched call returns instead, since every deffunction behind this helper always executes an
     * explicit {@code return} of its (possibly empty) string / multifield accumulator.
     */
    private String executeQueryTrimmed(String operation) {
        PrimitiveValue result = clips.eval(operation);
        if (result instanceof StringValue stringValue)
            return stringValue.stringValue();
        if (result instanceof MultifieldValue multifieldValue)
            return ((List<?>) multifieldValue.listValue()).stream().map(Object::toString).collect(Collectors.joining(" "));
        throw new IllegalStateException("CLIPS query did not return a string or multifield, got '" + result + "': " + operation);
    }

    /**
     * Evaluates a query whose successful result is always an integer (built via {@code bind ?number 0} before the
     * search, so "nothing matched" answers {@code "0"}, never {@link #CLIPS_FALSE}) and fails fast otherwise.
     */
    private String executeQuery(String operation) {
        String result = clips.eval(operation).toString();
        if (CLIPS_FALSE.equals(result))
            throw new IllegalStateException("CLIPS query failed (returned FALSE): " + operation);
        return result;
    }

    // ==================== FIRE LINE HYDRANTS ====================

    /**
     * Executes the CLIPS function {@code (get-fire-line-hydr ?location hydrants-here)} and returns the current number
     * of hydrants already assigned / available at the given fire-line location.
     * <p>
     * The function looks up the {@code fire-line-location} fact for the specified location and returns the current
     * value of its {@code hydrants-here} slot (an integer count). This value is populated by the forces arrangement /
     * extinguishing graph rules during the LOCALIZATION phase.
     * <p>
     * "Here" is CLIPS's own greedy hydrant-routing allocation (which specific hydrant outlets got assigned, not yet a
     * confirmation that a hose was physically run) — this is exactly the count
     * {@code gui.map.view.FireHoseButtonGroup}'s buttons mirror one-for-one (each button addresses one specific
     * allocated {@code HydrantOutlets}, see its javadoc for the dormant feature this was seeded for).
     * {@code hydrants-here} and {@code hydrants-need} move in lockstep during allocation — see
     * {@link #getFireLineHydrantsNeeded}.
     *
     * @param locationCode single-character location code of a fire-line location @return the number of hydrants here
     * (e.g. 2)
     */
    int getFireLineHydrantsPresent(String locationCode) {
        return Integer.parseInt(executeQueryTrimmed("(get-fire-line-hydr " + locationCode + " hydrants-here)"));
    }

    /**
     * Executes the CLIPS function {@code (get-fire-line-hydr ?location hydrants-need)} and returns the
     * still-outstanding number of hydrants for the given fire-line location.
     * <p>
     * The function looks up the {@code fire-line-location} fact for the specified location and returns the current
     * value of its {@code hydrants-need} slot (an integer count).
     * <p>
     * Not a static total — {@code hydrants-need} is first computed as {@code round(0.5 + perimeterLength/8)} when the
     * fire-line location is established (feis.clp {@code FIRE-LINES::regroup}, from the summed length of its
     * defense-perimeter borders), then decremented by exactly as much as {@code hydrants-here} (see
     * {@link #getFireLineHydrantsPresent}) increases during {@code HYDRANTS-ALLOCATION}'s hose routing — so
     * {@code here + need} is an invariant equal to the original total requirement, not a coincidence (mirrored by
     * {@code gui.map.view.controls.FrontlineBalanceLabel#setNumbers}'s own {@code total = here + need}). A location
     * where allocation could not fully satisfy the requirement (no hose route found for the remainder) keeps a nonzero
     * {@code need} in the final snapshot too — confirmed empirically, e.g. locations I / O in scenario D.
     *
     * @param locationCode single-character location code of a fire-line location @return the number of hydrants still
     * needed (e.g. 2)
     */
    int getFireLineHydrantsNeeded(String locationCode) {
        return Integer.parseInt(executeQueryTrimmed("(get-fire-line-hydr " + locationCode + " hydrants-need)"));
    }

    // ==================== HYDRANT OUTLETS ====================

    /**
     * Executes the CLIPS function {@code (get-hydrant-free-outs ?title)} and returns the number of free hydrant
     * connections available for the given hydrant title.
     * <p>
     * The function looks up the {@code HYDRANT} instance(s) with the matching {@code title} and returns the current
     * value of their {@code free} slot (how many hose lines can still be connected to this hydrant).
     * <p>
     * Typical titles (from {@link domain.HydrantOutlets#getTitle}):
     * <ul>
     * <li> {@code "hydr_d1"}, {@code "hydr_d2"} </li> <li> {@code "hydr_f "}, {@code "hydr_j "}, {@code "hydr_m "},
     * {@code "hydr_p "}, {@code "hydr_q "} </li>
     * </ul>
     *
     * @param hydrLabel the title of the hydrant (e.g."hydr_d1") @return the number of free connections (e.g. 2)
     */
    int getHydrantFreeOutlets(String hydrLabel) {
        return Integer.parseInt(executeQuery("(get-hydrant-free-outs " + hydrLabel + ")"));
    }

    /**
     * Executes a CLIPS query that returns the total number of connection slots for the given hydrant title (slot
     * {@code number} of the {@code HYDRANT} instance).
     */
    int getHydrantTotalOutlets(String hydrLabel) {
        return Integer.parseInt(executeQuery("(get-hydrant-total-outs " + hydrLabel + ")"));
    }

    // ==================== EXTINGUISHERS ====================

    /**
     * Executes the CLIPS function {@code (get-extinguishers-for-location ?location)} and returns the unused (
     * {@code used no}) portable extinguishers at the given location — the same guard the
     * {@code IMMEDIATE-EXTINGUISHERS::use-local} printout rule uses, so this read path mirrors exactly what that rule
     * already recommends.
     *
     * @param locationCode single-character location code (e.g."A","J") @return the matching extinguishers (may be
     * empty)
     */
    List<Extinguisher> getExtinguishersForLocation(String locationCode) {
        return parseExtinguishers(topology, executeQueryTrimmed("(get-extinguishers-for-location " + locationCode + ")"));
    }

    // ==================== HOSE ROUTING QUERIES (ext-graph / ext-edge — unrelated to EXTINGUISHER) ====================

    /**
     * Executes the CLIPS function {@code (get-ext-for-location ?location)} and returns the hydrant outlets assigned via
     * direct {@code ext-edge} connections ({@code hydrants-titles} multislot). In practice always empty today: the rule
     * base builds the graph but never fills those multislots (abandoned assignment half; see
     * {@code clips/INACTIVE.md}).
     *
     * @param locationCode single-character location code (e.g."A","F","Q") @return the assigned hydrant outlets (may be
     * empty)
     */
    List<HydrantOutlets> getExtForLocation(String locationCode) {
        return parseHydrantOutlets(topology, executeQueryTrimmed("(get-ext-for-location " + locationCode + ")"));
    }

    /**
     * Executes the CLIPS function {@code (get-ext-b-to-for-location ?location)} and returns hydrant outlets from
     * {@code ext-graph} facts where {@code to} matches the fire location (border-to direction). Wired into
     * {@code FireIncidentSnapshot#extBToByLocation} and {@code gui.map.view} HydrExt groups, but always empty at
     * runtime until the abandoned assignment rules are finished (see {@code clips/INACTIVE.md}).
     *
     * @param locationCode single-character location code of the fire location @return the matching hydrant outlets from
     * border-to graph edges (may be empty)
     */
    List<HydrantOutlets> getExtBToForLocation(String locationCode) {
        return parseHydrantOutlets(topology, executeQueryTrimmed("(get-ext-b-to-for-location " + locationCode + ")"));
    }

    /**
     * Executes the CLIPS function {@code (get-ext-b-from-for-location ?location)} and returns hydrant outlets from
     * {@code ext-graph} facts where {@code from} matches the location (border-from direction). Paired with
     * {@link #getGraphFromLocations()}; same inactive assignment path as {@link #getExtBToForLocation} (see
     * {@code clips/INACTIVE.md}).
     *
     * @param locationCode single-character location code (source of the ext-graph edge) @return the matching hydrant
     * outlets from border-from graph edges (may be empty)
     */
    List<HydrantOutlets> getExtBFromForLocation(String locationCode) {
        return parseHydrantOutlets(topology, executeQueryTrimmed("(get-ext-b-from-for-location " + locationCode + ")"));
    }

    /**
     * Executes the CLIPS function {@code (get-hydr-for-location ?location)} and returns the hydrant outlets assigned to
     * the given fire-line location.
     * <p>
     * The function looks up the {@code fire-line-location} fact for the specified location and returns the current
     * value of its {@code hydrants-titles} multislot. This multislot is populated by the extinguishing graph / forces
     * arrangement rules during the LOCALIZATION phase and contains the concrete hydrant titles that the algorithm
     * recommends for that fire-line compartment.
     * <p>
     * <b>Important distinction:</b> this method returns the assigned <i>hydrants</i> for the textual report. For the
     * visual map in the UI the separate functions {@code get-fire-line-hydr ... hydrants-here} and
     * {@code get-fire-line-hydr ... hydrants-need} are used (they return counts, not hydrants).
     *
     * @param locationCode single-character location code of a fire-line location (one of the codes returned by
     * {@link #getFireLineLocations()}) @return the hydrant outlets assigned to this fire line (may be empty)
     */
    List<HydrantOutlets> getHydrantsForLocation(String locationCode) {
        return parseHydrantOutlets(topology, executeQueryTrimmed("(get-hydr-for-location " + locationCode + ")"));
    }

    /**
     * Returns all source locations ({@code from} slot) from {@code ext-graph} facts. Does not return a structured graph
     * — only the set of starting locations. Paired with {@link #getExtBFromForLocation}; empty in practice for the same
     * abandoned assignment path (see {@code clips/INACTIVE.md}).
     */
    Set<Location> getGraphFromLocations() {
        return parseLocations(topology, executeQueryTrimmed("(get-graph-from-locations)"));
    }

    // ==================== FIREFIGHTING PLAN (EXTINGUISHING-PLAN phase) ====================

    /**
     * Executes the CLIPS function {@code (get-plan-from ?location)} and returns the source location of the firefighting
     * step for the given fire location.
     * <p>
     * The value is taken from the {@code from} slot of the {@code plan} fact created during the EXTINGUISHING-PLAN
     * phase. It represents the compartment from which firefighters should move to reach the current location according
     * to the generated extinguishing route.
     * <p>
     * The successful answer is a raw {@code from} multislot (a {@link MultifieldValue} wrapping a single
     * {@code SymbolValue}, e.g. {@code (j)}), not a {@code str-cat} -built string. {@code get-plan-from} has no
     * fallback {@code return}, so "no plan exists yet for this location" — a real, confirmed scenario (at least one
     * fire-line location per incident may not have a computed route from a neighbor yet) — answers the bare
     * {@link #CLIPS_FALSE} symbol instead of a multifield; this method maps that case to {@code null} rather than
     * rejecting it as a failure (the one place a {@code FALSE} answer is tolerated instead of thrown).
     *
     * @param locationCode single-character location code of a room on fire (e.g."A","F","Q") @return the previous
     * location in the firefighting path, or {@code null} if no plan exists for the specified location
     */
    Location getStepFrom(String locationCode) {
        PrimitiveValue result = clips.eval("(get-plan-from " + locationCode + ")");
        if (result instanceof MultifieldValue multifield && !multifield.listValue().isEmpty())
            return topology.location(multifield.listValue().get(0).toString());
        return null;
    }

    /**
     * Executes the CLIPS function {@code (get-plan-number ?location)} and returns the step number of the firefighting
     * plan for the given fire location.
     * <p>
     * The value comes from the {@code number} slot of the {@code plan} fact. This number indicates the sequential order
     * of the step within the overall extinguishing plan generated for the current fire scenario.
     * <p>
     * {@code get-plan-number} shares {@code get-plan-from}'s gap (no fallback return, so "no plan yet" also answers
     * bare {@code FALSE}), but unlike {@link #getStepFrom} this method has no established "empty" contract to fall back
     * to. {@code 0} is used as the placeholder pending confirmation of the intended UI behavior for a fire-line
     * location with no computed step yet, to avoid crashing here in the meantime.
     *
     * @param locationCode single-character location code of a room on fire @return the plan step number (e.g. 1, 2, 3),
     * or {@code 0} if no plan step exists yet for the specified location
     */
    int getStepNumber(String locationCode) {
        String result = clips.eval("(get-plan-number " + locationCode + ")").toString();
        return CLIPS_FALSE.equals(result) ? 0 : Integer.parseInt(result);
    }

    // ==================== COLLECTORS ====================

    /**
     * Executes the CLIPS function {@code (collect-evac-accedent ?slot-value)} and returns the locations that have the
     * specified {@code accedent} value.
     * <p>
     * Typical values: {@code "fire "}, {@code "threat "}.
     *
     * @param accidentValue the desired value of the {@code accedent} slot @return the matching locations (may be empty)
     */
    Set<Location> collectLocationsByStatus(String accidentValue) {
        return parseLocations(topology, executeQueryTrimmed("(collect-evac-accedent " + accidentValue + ")"));
    }

    /**
     * Executes the CLIPS function {@code (collect-evac-evacuation ?slot-value)} and returns the locations whose
     * {@code evacuation} slot matches the given value — rooms that need evacuation or have already been evacuated.
     * <p>
     * Typical values: {@code "to-evacuate "}, {@code "done "}.
     *
     * @param status the desired evacuation status @return the matching locations (may be empty)
     */
    Set<Location> collectEvacuationLocations(String status) {
        return parseLocations(topology, executeQueryTrimmed("(collect-evac-evacuation " + status + ")"));
    }

    /**
     * Executes the CLIPS function {@code (collect-germ-loc ?action)} and returns the locations whose {@code ventil}
     * slot matches the given value.
     * <p>
     * Used primarily to identify rooms where ventilation must be turned off during sealing.
     *
     * @param action the desired value of the {@code ventil} slot (typically {@code "to-off "}) @return the matching
     * locations (may be empty)
     */
    Set<Location> collectVentilationLocations(String action) {
        return parseLocations(topology, executeQueryTrimmed("(collect-germ-loc " + action + ")"));
    }

    /**
     * Executes the CLIPS function {@code (collect-germ-door ?action)} and returns the doors whose {@code status}
     * matches the given value (endpoints sorted alphabetically via {@code arrange-letters}, matching {@link Link}'s own
     * normalization).
     * <p>
     * Commonly used values for {@code action}:
     * <ul>
     * <li> {@code "to-close "} — doors that must be closed for sealing</li> <li> {@code "keep-open "} — doors that must
     * remain open to allow fire hose routing</li>
     * </ul>
     *
     * @param action the desired door status value (e.g."to-close","keep-open") @return the matching doors (may be
     * empty)
     */
    List<Link> collectDoorsToSeal(String action) {
        return parseLocationLinks(topology, executeQueryTrimmed("(collect-germ-door " + action + ")"));
    }

    /**
     * Executes the CLIPS function {@code (collect-action-phase ?phase)} and returns the locations from {@code ACTION}
     * facts that belong to the given phase and have not yet been marked as {@code done}.
     * <p>
     * This method is used for both explosion prevention and flammable-material prevention phases. Only actions with
     * {@code to-do != done} are included.
     *
     * @param phase the action phase ({@code "explosion "} or {@code "isolation "}) @return the locations that still
     * require action (may be empty)
     */
    Set<Location> collectActionPhase(String phase) {
        return parseLocations(topology, executeQueryTrimmed("(collect-action-phase " + phase + ")"));
    }

    /**
     * Executes the CLIPS function {@code (collect-isol-mech ?slot-value)} and returns the locations where the
     * {@code machinery} slot matches the given value.
     * <p>
     * Used to identify rooms where machinery must be stopped to prevent damage.
     *
     * @param action the desired value of the {@code machinery} slot (typically {@code "stop "}) @return the matching
     * locations (may be empty)
     */
    Set<Location> collectMachineryDamageLocations(String action) {
        return parseLocations(topology, executeQueryTrimmed("(collect-isol-mech " + action + ")"));
    }

    /**
     * Executes the CLIPS function {@code (get-fire-line-locations)} and returns every location for which a
     * {@code fire-line-location} fact exists (i.e. compartments that form the fire line /рубеж обороны).
     *
     * @return the fire-line locations (may be empty)
     */
    Set<Location> getFireLineLocations() {
        return parseLocations(topology, executeQueryTrimmed("(get-fire-line-locations)"));
    }

    /**
     * Executes the CLIPS function {@code (get-line1-borders)} and returns the doors that form the first fire line
     * border (endpoints sorted alphabetically via {@code arrange-letters}).
     *
     * @return the first-fire-line border doors (may be empty)
     */
    List<Link> getFrontLineBorders() {
        return parseLocationLinks(topology, executeQueryTrimmed("(get-line1-borders)"));
    }

    // ==================== EXPLANATION QUERIES ====================

    /**
     * Executes the CLIPS function {@code (get-explanation-evac ?slot ?slot-value)} and returns the value of the
     * specified slot from the {@code EXPLAIN} instance created for evacuation of the given location.
     * <p>
     * Typically used to retrieve human-readable explanations (antecedent / consequent) why a particular room must be
     * evacuated.
     *
     * @param slot the name of the slot to retrieve (e.g. {@code "antec "}, {@code "consec "}) @param slotValue the
     * location code for which the explanation is requested @return the string value of the requested slot (may be
     * empty)
     */
    String getExplanationEvacuation(String slot, String slotValue) {
        return executeQueryTrimmed("(get-explanation-evac " + slot + " " + slotValue + ")");
    }

    /**
     * Executes the CLIPS function {@code (get-explanation ?slot ?slot-value)} and returns the value of the specified
     * slot from the {@code EXPLAIN} instance related to sealing for the given location.
     *
     * @param slot the name of the slot to retrieve @param slotValue the location code @return the string value of the
     * requested slot (may be empty)
     */
    String getExplanation(String slot, String slotValue) {
        return executeQueryTrimmed("(get-explanation " + slot + " " + slotValue + ")");
    }

    /**
     * Executes the CLIPS function {@code (get-explanation2 ?slot ?slot1-value ?slot2-value)} and returns the value of
     * the specified slot from the {@code EXPLAIN} instance for sealing between two locations (uses {@code from} and
     * {@code to} slots).
     *
     * @param slot the name of the slot to retrieve @param fromLocation the "from" location code @param toLocation the
     * "to" location code @return the string value of the requested slot (may be empty)
     */
    String getExplanation(String slot, String fromLocation, String toLocation) {
        return executeQueryTrimmed("(get-explanation2 " + slot + " " + fromLocation + " " + toLocation + ")");
    }

    /**
     * Executes the CLIPS function {@code (get-explanation-expl ?slot ?slot-value)} and returns the value of the
     * specified slot from the {@code EXPLAIN} instance created for explosion prevention actions.
     *
     * @param slot the name of the slot to retrieve @param slotValue the location code @return the string value of the
     * requested slot (may be empty)
     */
    String getExplanationForExplosions(String slot, String slotValue) {
        return executeQueryTrimmed("(get-explanation-expl " + slot + " " + slotValue + ")");
    }

    /**
     * Executes the CLIPS function {@code (get-explanation-isol ?slot ?slot-value)} and returns the value of the
     * specified slot from the {@code EXPLAIN} instance created for flammable-material ignition prevention.
     *
     * @param slot the name of the slot to retrieve @param slotValue the location code @return the string value of the
     * requested slot (may be empty)
     */
    String getExplanationForFlammable(String slot, String slotValue) {
        return executeQueryTrimmed("(get-explanation-isol " + slot + " " + slotValue + ")");
    }

    /**
     * Executes the CLIPS function {@code (get-explanation-isol-mech ?slot ?slot-value)} and returns the value of the
     * specified slot from the {@code EXPLAIN} instance created for machinery damage prevention ({@code type = mech}).
     *
     * @param slot the name of the slot to retrieve @param slotValue the location code @return the string value of the
     * requested slot (may be empty)
     */
    String getExplanationForMachineryDamage(String slot, String slotValue) {
        return executeQueryTrimmed("(get-explanation-isol-mech " + slot + " " + slotValue + ")");
    }

    // ==================== REPORTING (sending changes back to CLIPS) ====================

    void reportVentilation(String locationCode, String value) {
        evalOrThrow(sendMessage(locationCode, "put-ventil", value));
    }

    void reportDoorStatus(Link door, String value) {
        String doorName = doorInstanceName(door.getFrom().getCode(), door.getTo().getCode());
        evalOrThrow(sendMessage(doorName, "put-status", value));
    }

    void reportEvacuation(String locationCode, String value) {
        evalOrThrow(sendMessage(locationCode, "put-evacuation", value));
    }

    /**
     * Not hardened like the {@code send} -based reporters above: {@code action-edit} answers {@code FALSE} both when
     * the dispatch fails <em>and</em> when there is simply no matching {@code ACTION} fact to update (e.g. already
     * reported done) — a legitimate, common outcome — so its {@code FALSE} is not a reliable failure signal on its own.
     * See {@link #evalOrThrow}.
     */
    void reportFlammablePrevention(String locationCode, String value) {
        clips.eval("(action-edit isolation " + locationCode + " " + value + ")");
    }

    /**
     * See {@link #reportFlammablePrevention} — {@code action-edit}'s {@code FALSE} is ambiguous, not hardened.
     */
    void reportExplosionPrevention(String locationCode, String value) {
        clips.eval("(action-edit explosion " + locationCode + " " + value + ")");
    }

    void reportMachineryStop(String locationCode) {
        evalOrThrow(sendMessage(locationCode, "put-machinery", "stop"));
    }

    void reportMachineryDone(String locationCode) {
        evalOrThrow(sendMessage(locationCode, "put-machinery", "done"));
    }

    /**
     * Reports a portable extinguisher as used or not used. {@code title} is also the CLIPS instance name (see
     * {@link #fromExtinguisherInstance}), addressed directly — unlike {@link #reportDoorStatus}, there is no separate
     * naming convention to apply here.
     */
    void reportExtinguisherUsed(String title, String value) {
        evalOrThrow(sendMessage(title, "put-used", value));
    }
}
