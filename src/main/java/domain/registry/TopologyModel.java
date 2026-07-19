package domain.registry;

import domain.Border;
import domain.Door;
import domain.EvacuationRoute;
import domain.Extinguisher;
import domain.FireHoseSpan;
import domain.FireSensor;
import domain.HydrantOutlets;
import domain.Link;
import domain.Location;
import domain.types.ExtinguisherType;
import domain.types.FireSensorType;

import java.util.List;
import java.util.Map;

/**
 * Aggregate of the nine topology registries ({@link LocationRegistry}, {@link LinkRegistry},
 * {@link FireSensorRegistry}, {@link HydrantOutletsRegistry}, {@link EvacuationRouteRegistry},
 * {@link FireHoseSpanRegistry}, {@link ExtinguisherRegistry}, {@link BorderRegistry}, {@link DoorRegistry}), assembled
 * once in the composition root and injected wherever raw codes from CLIPS output or configuration must be resolved into
 * domain value objects. This is the single source of truth {@code clips.ClipsEngineAccess#initializeTopology} seeds the
 * CLIPS engine from — including {@link Border}/{@link Door}, so callers no longer pass separate config-level border /
 * door lists alongside it.
 * <p>
 * This is a plain injected instance — there is deliberately no static {@code INSTANCE}. The individual registries
 * remain accessible (and independently testable) via the {@code *Registry()} accessors, while the
 * {@code location}/{@code link}/… shortcuts keep call sites terse.
 */
public final class TopologyModel {
    private final LocationRegistry locations;
    private final LinkRegistry links;
    private final FireSensorRegistry fireSensors;
    private final HydrantOutletsRegistry hydrants;
    private final EvacuationRouteRegistry evacuationRoutes;
    private final FireHoseSpanRegistry fireHoseSpans;
    private final ExtinguisherRegistry extinguishers;
    private final BorderRegistry borders;
    private final DoorRegistry doors;

    // Aggregate of nine typed registries; each parameter is a distinct type — S107 not actionable.
    @SuppressWarnings("java:S107")
    public TopologyModel(LocationRegistry locations, LinkRegistry links, FireSensorRegistry fireSensors,
        HydrantOutletsRegistry hydrants, EvacuationRouteRegistry evacuationRoutes, FireHoseSpanRegistry fireHoseSpans,
        ExtinguisherRegistry extinguishers, BorderRegistry borders, DoorRegistry doors)
    {
        this.locations = locations;
        this.links = links;
        this.fireSensors = fireSensors;
        this.hydrants = hydrants;
        this.evacuationRoutes = evacuationRoutes;
        this.fireHoseSpans = fireHoseSpans;
        this.extinguishers = extinguishers;
        this.borders = borders;
        this.doors = doors;
    }

    /**
     * Builds the whole model from a {@link RawTopology}, wiring the registries in the required order (locations first,
     * since links, sensors, evacuation routes and extinguishers resolve against them, and fire-hose spans / borders
     * resolve against links, and doors resolve against locations). See {@link RawTopology} field javadoc-equivalent
     * names for which raw section feeds which registry.
     */
    public static TopologyModel from(RawTopology blueprint) {
        LocationRegistry locations = new LocationRegistry(blueprint.locations());
        LinkRegistry links = new LinkRegistry(blueprint.linkCodes(), locations);
        FireSensorRegistry fireSensors = new FireSensorRegistry(blueprint.sensorTypes(), locations);
        HydrantOutletsRegistry hydrants = new HydrantOutletsRegistry(blueprint.hydrantOutletCounts(), locations);
        EvacuationRouteRegistry evacuationRoutes = new EvacuationRouteRegistry(blueprint.evacuationRouteCodes(), locations);
        FireHoseSpanRegistry fireHoseSpans = new FireHoseSpanRegistry(
            blueprint.doorToDoorSpans(), blueprint.hydrantToDoorSpans(), links, hydrants);
        ExtinguisherRegistry extinguishers = new ExtinguisherRegistry(blueprint.extinguisherTypes(), locations);
        BorderRegistry borders = new BorderRegistry(blueprint.borders(), links);
        DoorRegistry doors = new DoorRegistry(blueprint.doors(), locations);
        return new TopologyModel(locations, links, fireSensors, hydrants, evacuationRoutes, fireHoseSpans,
            extinguishers, borders, doors);
    }

    public Location location(String code) {
        return locations.get(code);
    }

    public Link link(String code) {
        return links.get(code);
    }

    public FireSensor fireSensor(String code) {
        return fireSensors.get(code);
    }

    public HydrantOutlets hydrantOutlets(String title) {
        return hydrants.get(title);
    }

    public EvacuationRoute evacuationRoute(String directedCode) {
        return evacuationRoutes.get(directedCode);
    }

    public Extinguisher extinguisher(String title) {
        return extinguishers.get(title);
    }

    public List<Location> allLocations() {
        return locations.all();
    }

    public List<Link> allLinks() {
        return links.all();
    }

    public List<FireSensor> allFireSensors() {
        return fireSensors.all();
    }

    public List<HydrantOutlets> allHydrantOutlets() {
        return hydrants.all();
    }

    public List<EvacuationRoute> allEvacuationRoutes() {
        return evacuationRoutes.all();
    }

    public List<FireHoseSpan<Link>> allDoorToDoorFireHoseSpans() {
        return fireHoseSpans.allDoorToDoor();
    }

    public List<FireHoseSpan<HydrantOutlets>> allHydrantToDoorFireHoseSpans() {
        return fireHoseSpans.allHydrantToDoor();
    }

    public List<Extinguisher> allExtinguishers() {
        return extinguishers.all();
    }

    public List<Border> allBorders() {
        return borders.all();
    }

    public List<Door> allDoors() {
        return doors.all();
    }

    /**
     * Named bag of raw topology inputs for {@link TopologyModel#from(RawTopology)}. Replaces a ten-argument positional
     * {@code from(...)} (many same-typed {@code List}/{@code Map} slots) so callers cannot silently swap sections.
     * Field names mirror the topology.yaml concerns that
     * {@link config.loading.DeckMapTopologyConfig#buildTopologyModel()} already assembles.
     */
    public record RawTopology(
        List<LocationRegistry.RawLocation> locations,
        List<String> linkCodes,
        Map<String, FireSensorType> sensorTypes,
        Map<String, Integer> hydrantOutletCounts,
        List<String> evacuationRouteCodes,
        List<FireHoseSpanRegistry.RawSpan> doorToDoorSpans,
        List<FireHoseSpanRegistry.RawSpan> hydrantToDoorSpans,
        Map<String, ExtinguisherType> extinguisherTypes,
        List<BorderRegistry.RawBorder> borders,
        List<DoorRegistry.RawDoor> doors
    ) {
        /**
         * Empty blueprint (no locations, links, equipment, or routes) — useful as a base for tests that only fill the
         * sections they exercise via {@code with*}.
         */
        public static RawTopology empty() {
            return new RawTopology(
                List.of(), List.of(), Map.of(), Map.of(), List.of(),
                List.of(), List.of(), Map.of(), List.of(), List.of());
        }

        public RawTopology withLocations(List<LocationRegistry.RawLocation> locations) {
            return new RawTopology(locations, linkCodes, sensorTypes, hydrantOutletCounts,
                evacuationRouteCodes, doorToDoorSpans, hydrantToDoorSpans, extinguisherTypes, borders, doors);
        }

        public RawTopology withLinkCodes(List<String> linkCodes) {
            return new RawTopology(locations, linkCodes, sensorTypes, hydrantOutletCounts,
                evacuationRouteCodes, doorToDoorSpans, hydrantToDoorSpans, extinguisherTypes, borders, doors);
        }

        public RawTopology withHydrantOutletCounts(Map<String, Integer> hydrantOutletCounts) {
            return new RawTopology(locations, linkCodes, sensorTypes, hydrantOutletCounts,
                evacuationRouteCodes, doorToDoorSpans, hydrantToDoorSpans, extinguisherTypes, borders, doors);
        }

        public RawTopology withExtinguisherTypes(Map<String, ExtinguisherType> extinguisherTypes) {
            return new RawTopology(locations, linkCodes, sensorTypes, hydrantOutletCounts,
                evacuationRouteCodes, doorToDoorSpans, hydrantToDoorSpans, extinguisherTypes, borders, doors);
        }
    }
}
