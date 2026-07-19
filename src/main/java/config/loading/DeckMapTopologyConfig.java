package config.loading;

import com.fasterxml.jackson.annotation.*;
import config.YamlConfigLoader;
import config.specification.basic.FireHoseSpansSpec;
import config.specification.basic.*;
import config.specification.basic.HydrantOutletSpec;
import domain.registry.BorderRegistry;
import domain.registry.DoorRegistry;
import domain.registry.LinkRegistry;
import domain.types.FlammableMaterial;
import domain.types.CompartmentType;
import domain.types.ExplosiveMaterial;
import domain.types.ExtinguisherType;
import domain.types.FireSensorType;
import domain.Location;
import domain.types.VentilationType;
import domain.registry.FireHoseSpanRegistry;
import domain.registry.LocationRegistry;
import domain.registry.TopologyModel;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeckMapTopologyConfig(
    @JsonProperty("location-labels") List<LocationSpec> locationsLabels,
    @JsonProperty("borders") List<BorderSpec> borders,
    @JsonProperty("doors") List<DoorSpec> doors,
    @JsonProperty("evacuation-routes") List<EvacuationRouteSpec> evacuationRoutes,
    @JsonProperty("fire-hose-spans") FireHoseSpansSpec fireHoseSpans,
    @JsonProperty("fire-sensor-codes") List<FireSensorSpec> sensorSpecs,
    @JsonProperty("hydrant-outlets") List<HydrantOutletSpec> hydrantOutlets,
    @JsonProperty("extinguishers") List<ExtinguisherSpec> extinguishers
) {
    /**
     * Loads the raw topology description from YAML. This is a pure data DTO: resolving the raw code lists into domain
     * value objects is the job of {@link TopologyModel}, built via {@link #buildTopologyModel()}.
     */
    public static DeckMapTopologyConfig createDefault() {
        return YamlConfigLoader.load("config/topology.yaml", DeckMapTopologyConfig.class);
    }

    /**
     * Resolves this raw config into the domain's {@link TopologyModel}. Kept on the config side (rather than as a
     * {@code TopologyModel} constructor) so {@code domain.registry} never needs to depend on the Jackson-facing config
     * DTOs — the dependency stays {@code config} → {@code domain.registry}, the same direction already used throughout
     * the app.
     */
    @JsonIgnore
    public TopologyModel buildTopologyModel() {
        Map<String, Integer> hydrantOutletCounts = hydrantOutlets.stream()
            .collect(toMap(HydrantOutletSpec::title, HydrantOutletSpec::outlets, (a, b) -> a, LinkedHashMap::new));
        List<String> evacuationRouteCodes = evacuationRoutes.stream()
            .map(route -> route.from() + route.to())
            .toList();
        Map<String, ExtinguisherType> extinguisherTypes = extinguishers.stream()
            .collect(toMap(ExtinguisherSpec::title, spec -> ExtinguisherType.fromName(spec.type()), (a, b) -> a, LinkedHashMap::new));
        Map<String, FireSensorType> sensorTypes = sensorSpecs.stream()
            .collect(toMap(FireSensorSpec::code, spec -> FireSensorType.fromName(spec.type()), (a, b) -> a, LinkedHashMap::new));
        return TopologyModel.from(new TopologyModel.RawTopology(
            toRawLocations(locationsLabels), resolveLinkCodes(), sensorTypes, hydrantOutletCounts,
            evacuationRouteCodes, toRawSpans(fireHoseSpans.doorToDoor()), toRawSpans(fireHoseSpans.hydrantToDoor()),
            extinguisherTypes, toRawBorders(), toRawDoors()));
    }

    private List<BorderRegistry.RawBorder> toRawBorders() {
        return borders.stream().map(b -> new BorderRegistry.RawBorder(b.link(), b.length())).toList();
    }

    private List<DoorRegistry.RawDoor> toRawDoors() {
        return doors.stream().map(d -> new DoorRegistry.RawDoor(d.from(), d.to())).toList();
    }

    /**
     * Resolves each raw {@link LocationSpec} into a {@link LocationRegistry.RawLocation}: an omitted attribute takes
     * the same default CLIPS's own {@code location-attrs} slot defaults used (
     * {@link Location#NO_AREA}/{@link Location#DEFAULT_TANK}/{@link CompartmentType#UNINHABITED}, no ventilation
     * system, booleans {@code false}, no explosive / burning material).
     */
    private static List<LocationRegistry.RawLocation> toRawLocations(List<LocationSpec> specs) {
        return specs.stream().map(DeckMapTopologyConfig::toRawLocation).toList();
    }

    private static LocationRegistry.RawLocation toRawLocation(LocationSpec spec) {
        return new LocationRegistry.RawLocation(
            spec.code(),
            spec.area() != null ? spec.area() : Location.NO_AREA,
            spec.tank() != null ? spec.tank() : Location.DEFAULT_TANK,
            spec.type() != null ? CompartmentType.fromName(spec.type()) : CompartmentType.UNINHABITED,
            spec.ventilation() != null ? VentilationType.fromName(spec.ventilation()) : null,
            spec.explosive() != null ? ExplosiveMaterial.fromName(spec.explosive()) : null,
            spec.burning() != null ? FlammableMaterial.fromName(spec.burning()) : null,
            Boolean.TRUE.equals(spec.machinery()),
            Boolean.TRUE.equals(spec.chemicalSuppression()));
    }

    private static List<FireHoseSpanRegistry.RawSpan> toRawSpans(List<FireHoseSpanSpec> specs) {
        return specs.stream()
            .map(spec -> new FireHoseSpanRegistry.RawSpan(spec.from(), spec.to(), spec.distance()))
            .toList();
    }

    /**
     * The link codes the {@link LinkRegistry} must resolve: every bulkhead ({@code borders}) plus every door that
     * connects two locations. Doors to another deck ({@code to = out}) are not links. Interior doors already coincide
     * with borders, so only the doors to adjacent compartments (R / T) contribute codes beyond the border set.
     */
    private List<String> resolveLinkCodes() {
        Set<String> codes = new LinkedHashSet<>();
        borders.forEach(border -> codes.add(border.link()));
        for (DoorSpec door : doors) {
            if (!DoorSpec.EXTERNAL_DECK.equalsIgnoreCase(door.to()))
                codes.add(door.from() + door.to());
        }
        return List.copyOf(codes);
    }

    @Override
    @JsonProperty("hydrant-outlets")
    public List<HydrantOutletSpec> hydrantOutlets() {
        return hydrantOutlets;
    }

    @Override
    @JsonProperty("extinguishers")
    public List<ExtinguisherSpec> extinguishers() {
        return extinguishers;
    }

    @Override
    @JsonProperty("location-labels")
    public List<LocationSpec> locationsLabels() {
        return locationsLabels;
    }

    @Override
    @JsonProperty("borders")
    public List<BorderSpec> borders() {
        return borders;
    }

    @Override
    @JsonProperty("doors")
    public List<DoorSpec> doors() {
        return doors;
    }

    @Override
    @JsonProperty("evacuation-routes")
    public List<EvacuationRouteSpec> evacuationRoutes() {
        return evacuationRoutes;
    }

    @Override
    @JsonProperty("fire-hose-spans")
    public FireHoseSpansSpec fireHoseSpans() {
        return fireHoseSpans;
    }

    @Override
    @JsonProperty("fire-sensor-codes")
    public List<FireSensorSpec> sensorSpecs() {
        return sensorSpecs;
    }

    /**
     * Border link codes in declaration order (indexes {@code geometry.yaml}'s {@code border-coordinates} for fire-line
     * rendering).
     */
    @JsonIgnore
    public List<String> getBorderCodes() {
        return borders.stream().map(BorderSpec::link).toList();
    }

    /**
     * Whether each door is fire-rated, keyed by its 2-letter link code (matching
     * {@code DoorGlyphSpec.doorCode()}/{@code Link.getCode()}). Doors to another deck have no such code (no button to
     * key) and are excluded. This is the single source of truth for the ordinary / fire-rated rendering distinction —
     * see {@code gui.map.input.controls.DoorSealingButton}.
     */
    @JsonIgnore
    public Map<String, Boolean> getFireRatedByDoorCode() {
        Map<String, Boolean> fireRatedByCode = new LinkedHashMap<>();
        for (DoorSpec door : doors) {
            if (!DoorSpec.EXTERNAL_DECK.equalsIgnoreCase(door.to()))
                fireRatedByCode.put(door.from() + door.to(), door.fireRated());
        }
        return fireRatedByCode;
    }
}
