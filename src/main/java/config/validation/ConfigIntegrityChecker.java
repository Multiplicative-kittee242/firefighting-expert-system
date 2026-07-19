package config.validation;

import config.loading.DeckMapControlsConfig;
import config.loading.DeckMapGeometryConfig;
import config.loading.DeckMapGroupsConfig;
import config.loading.DeckMapTopologyConfig;
import config.groups.DoorButtonGroupConfig;
import config.groups.HydrantsGroupConfig;
import config.groups.HydrOutletLabelGroupConfig;
import config.groups.ToggleGroupConfig;
import config.specification.*;
import config.specification.basic.*;
import config.specification.buttons.FireSensorButtonSpec;
import config.specification.basic.HydrLabelSpec;
import config.specification.basic.HydrantOutletSpec;
import config.specification.basic.HydrantPlacement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cross-file referential integrity checks on raw config DTOs (AFTER yaml load / validation, BEFORE TopologyModel
 * construction).
 * <p>
 * Uses raw specs from topology as source of truth (no dependency on domain.registry). Collects ALL violations then
 * throws single ConfigValidationException.
 * <p>
 * Does not depend on clips / gui / app (ArchitectureRulesTest invariant).
 */
public final class ConfigIntegrityChecker {
    private static final String WHAT_LOCATION = "location";
    private static final String LOCATION_NOT_FOUND_IN_TOPOLOGY = " — location not found in topology";
    private static final String LOCATION_CODE_FIELD = "locationCode";

    private ConfigIntegrityChecker() {}

    public static void check(DeckMapTopologyConfig topologyConfig, DeckMapControlsConfig controlsConfig,
        DeckMapGroupsConfig groupsConfig, DeckMapGeometryConfig geometryConfig)
    {
        List<String> violations = new ArrayList<>();

        Set<String> locationCodes = readLocationCodes(topologyConfig);
        Set<String> sensorCodes = readSensorCodes(topologyConfig);
        Set<String> hydrantTitles = readHydrantTitles(topologyConfig);
        Set<String> linkAndDoorCodes = readLinkAndDoorCodes(topologyConfig);

        checkControlsPlacements(controlsConfig, locationCodes, violations);
        checkGroupsReferences(groupsConfig, locationCodes, sensorCodes, hydrantTitles, linkAndDoorCodes, violations);
        checkGeometryMatchesTopology(topologyConfig, geometryConfig, violations);

        if (!violations.isEmpty())
            throw new ConfigValidationException(violations);
    }

    // --- source-of-truth sets from topology.yaml ---

    private static Set<String> readLocationCodes(DeckMapTopologyConfig topologyConfig) {
        if (topologyConfig.locationsLabels() == null)
            return Set.of();
        return topologyConfig.locationsLabels().stream()
            .map(LocationSpec::code)
            .collect(Collectors.toSet());
    }

    private static Set<String> readSensorCodes(DeckMapTopologyConfig topologyConfig) {
        if (topologyConfig.sensorSpecs() == null)
            return Set.of();
        return topologyConfig.sensorSpecs().stream()
            .map(FireSensorSpec::code)
            .collect(Collectors.toSet());
    }

    private static Set<String> readHydrantTitles(DeckMapTopologyConfig topologyConfig) {
        if (topologyConfig.hydrantOutlets() == null)
            return Set.of();
        return topologyConfig.hydrantOutlets().stream()
            .map(HydrantOutletSpec::title)
            .collect(Collectors.toSet());
    }

    private static Set<String> readLinkAndDoorCodes(DeckMapTopologyConfig topologyConfig) {
        Set<String> linkAndDoorCodes = new LinkedHashSet<>();
        if (topologyConfig.borders() != null) {
            for (BorderSpec b : topologyConfig.borders()) {
                if (b.link() != null)
                    linkAndDoorCodes.add(b.link());
            }
        }
        if (topologyConfig.doors() != null) {
            for (DoorSpec d : topologyConfig.doors()) {
                if (!DoorSpec.EXTERNAL_DECK.equalsIgnoreCase(d.to())) {
                    String code = d.from() + d.to();
                    linkAndDoorCodes.add(code);
                }
            }
        }
        return linkAndDoorCodes;
    }

    // --- controls.yaml placing sections ---

    private static void checkControlsPlacements(DeckMapControlsConfig controlsConfig, Set<String> locationCodes,
        List<String> violations)
    {
        final String CONTROLS = "config/controls.yaml";

        checkStringCodePlacements(controlsConfig.explosionPreventionMarkers(), locationCodes, violations, CONTROLS,
            "explosion-markers-placing", WHAT_LOCATION);
        checkStringCodePlacements(controlsConfig.fireButtons(), locationCodes, violations, CONTROLS,
            "fire-buttons-placing", WHAT_LOCATION);
        checkStringCodePlacements(controlsConfig.evacuationButtons(), locationCodes, violations, CONTROLS,
            "evacuation-buttons-placing", WHAT_LOCATION);
        checkHydrantPlacementCodes(controlsConfig.hydrantPlacement(), locationCodes, violations, CONTROLS,
            "hydrant-placing");
        checkDoorButtonPlacements(controlsConfig.doorButtons(), locationCodes, violations, CONTROLS,
            "door-buttons-placing");
    }

    // --- groups.yaml groups ---

    private static void checkGroupsReferences(DeckMapGroupsConfig groupsConfig, Set<String> locationCodes,
        Set<String> sensorCodes, Set<String> hydrantTitles, Set<String> linkAndDoorCodes, List<String> violations)
    {
        final String GROUPS = "config/groups.yaml";

        checkItemCodes(groupItems(groupsConfig.ventilationGroupConfig()), LocationAttached::locationCode,
            locationCodes, violations, GROUPS, "ventilation-group", LOCATION_CODE_FIELD, LOCATION_NOT_FOUND_IN_TOPOLOGY);
        checkItemCodes(groupItems(groupsConfig.flammableGroupConfig()), LocationAttached::locationCode,
            locationCodes, violations, GROUPS, "flammable-group", LOCATION_CODE_FIELD, LOCATION_NOT_FOUND_IN_TOPOLOGY);
        checkItemCodes(groupItems(groupsConfig.machineryDamageGroupConfig()), LocationAttached::locationCode,
            locationCodes, violations, GROUPS, "machinery-damage-group", LOCATION_CODE_FIELD, LOCATION_NOT_FOUND_IN_TOPOLOGY);
        checkItemCodes(groupItems(groupsConfig.explosionGroupConfig()), LocationAttached::locationCode,
            locationCodes, violations, GROUPS, "explosion-group", LOCATION_CODE_FIELD, LOCATION_NOT_FOUND_IN_TOPOLOGY);

        checkItemCodes(groupItems(groupsConfig.frontlineBalanceGroupConfig()), LocationAttached::locationCode,
            locationCodes, violations, GROUPS, "frontline-balance-label-group", LOCATION_CODE_FIELD,
            LOCATION_NOT_FOUND_IN_TOPOLOGY);
        checkItemCodes(groupItems(groupsConfig.firefightingStepGroupConfig()), LocationAttached::locationCode,
            locationCodes, violations, GROUPS, "firefighting-step-label-group", LOCATION_CODE_FIELD,
            LOCATION_NOT_FOUND_IN_TOPOLOGY);

        checkItemCodes(groupItems(groupsConfig.fireSensorGroupConfig()), FireSensorButtonSpec::sensorCode,
            sensorCodes, violations, GROUPS, "fire-sensor-group", "sensorCode",
            " — sensor not found in topology");

        checkItemCodes(groupItems(groupsConfig.hydrOutletLabelGroupConfig()), HydrLabelSpec::titleCode,
            hydrantTitles, violations, GROUPS, "hydrant-outlet-label-group", "titleCode",
            " — hydrant title not found in topology");

        checkItemCodes(groupItems(groupsConfig.doorButtonGroup()), DoorGlyphSpec::doorCode,
            linkAndDoorCodes, violations, GROUPS, "door-button-group", "code",
            " — door/link code not found in topology");
    }

    // --- geometry.yaml ↔ topology.yaml ---

    private static void checkGeometryMatchesTopology(DeckMapTopologyConfig topologyConfig,
        DeckMapGeometryConfig geometryConfig, List<String> violations)
    {
        final String GEOM = "config/geometry.yaml";
        final String TOPO = "config/topology.yaml";

        Map<String, Integer> codeToTank = buildCodeToTank(topologyConfig);
        Map<Integer, Set<String>> locationsByTank = groupLocationsByTank(codeToTank);
        checkGeometryLocationCoordinates(geometryConfig, codeToTank, locationsByTank, violations, GEOM);
        checkBorderLinksMatch(topologyConfig, geometryConfig, violations, GEOM, TOPO);
    }

    private static Map<String, Integer> buildCodeToTank(DeckMapTopologyConfig topologyConfig) {
        Map<String, Integer> codeToTank = new LinkedHashMap<>();
        if (topologyConfig.locationsLabels() != null) {
            for (LocationSpec loc : topologyConfig.locationsLabels()) {
                if (loc.code() != null)
                    codeToTank.put(loc.code().toUpperCase(), loc.tank());
            }
        }
        return codeToTank;
    }

    private static Map<Integer, Set<String>> groupLocationsByTank(Map<String, Integer> codeToTank) {
        Map<Integer, Set<String>> locationsByTank = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : codeToTank.entrySet()) {
            Integer tank = e.getValue();
            locationsByTank.computeIfAbsent(tank, k -> new LinkedHashSet<>()).add(e.getKey());
        }
        return locationsByTank;
    }

    private static void checkGeometryLocationCoordinates(DeckMapGeometryConfig geometryConfig,
        Map<String, Integer> codeToTank, Map<Integer, Set<String>> locationsByTank, List<String> violations,
        String geomFile)
    {
        Map<Integer, Set<String>> geoByTank = buildGeoLocationsByTank(geometryConfig, codeToTank, violations, geomFile);
        checkTankAllOrNone(locationsByTank, geoByTank, violations, geomFile);
    }

    private static Map<Integer, Set<String>> buildGeoLocationsByTank(DeckMapGeometryConfig geometryConfig,
        Map<String, Integer> codeToTank, List<String> violations, String geomFile)
    {
        Map<Integer, Set<String>> geoByTank = new LinkedHashMap<>();
        if (geometryConfig == null || geometryConfig.locationCoordinates() == null)
            return geoByTank;

        for (LocationCoordinateSpec spec : geometryConfig.locationCoordinates()) {
            if (spec != null && spec.code() != null) {
                String code = spec.code().toUpperCase();
                Integer tank = codeToTank.get(code);
                if (tank != null) {
                    geoByTank.computeIfAbsent(tank, k -> new LinkedHashSet<>()).add(code);
                } else {
                    violations.add(geomFile + ": location-coordinates/code=" + spec.code() +
                        " — code not found in topology location-labels");
                }
            }
        }
        return geoByTank;
    }

    private static void checkTankAllOrNone(Map<Integer, Set<String>> locationsByTank,
        Map<Integer, Set<String>> geoByTank, List<String> violations, String geomFile)
    {
        for (Map.Entry<Integer, Set<String>> e : locationsByTank.entrySet()) {
            Integer tank = e.getKey();
            Set<String> tankLocs = e.getValue();
            Set<String> tankGeo = geoByTank.getOrDefault(tank, Set.of());

            if (!tankGeo.isEmpty() && !tankGeo.equals(tankLocs)) {
                violations.add(geomFile + ": for tank " + tank +
                    " geometry codes " + tankGeo + " do not exactly match topology locations " + tankLocs +
                    " (must be complete match or completely absent)");
            }
        }
    }

    private static void checkBorderLinksMatch(DeckMapTopologyConfig topologyConfig,
        DeckMapGeometryConfig geometryConfig, List<String> violations, String geomFile, String topoFile)
    {
        Set<String> topoBorderLinks = buildTopoBorderLinks(topologyConfig);
        Set<String> geoBorderLinks = buildGeoBorderLinksAndValidate(geometryConfig, topoBorderLinks, violations, geomFile);

        if (!geoBorderLinks.equals(topoBorderLinks)) {
            violations.add(geomFile + " border-coordinates links " + geoBorderLinks + " do not exactly match "
                + topoFile + " borders " + topoBorderLinks);
        }
    }

    private static Set<String> buildTopoBorderLinks(DeckMapTopologyConfig topologyConfig) {
        Set<String> topoBorderLinks = new LinkedHashSet<>();
        if (topologyConfig.borders() == null)
            return topoBorderLinks;

        for (BorderSpec b : topologyConfig.borders()) {
            if (b.link() != null)
                topoBorderLinks.add(b.link().toUpperCase());
        }
        return topoBorderLinks;
    }

    private static Set<String> buildGeoBorderLinksAndValidate(DeckMapGeometryConfig geometryConfig,
        Set<String> topoBorderLinks, List<String> violations, String geomFile)
    {
        Set<String> geoBorderLinks = new LinkedHashSet<>();
        if (geometryConfig == null || geometryConfig.borderCoordinates() == null)
            return geoBorderLinks;

        for (BorderCoordinateSpec spec : geometryConfig.borderCoordinates()) {
            if (spec != null && spec.link() != null) {
                String lnk = spec.link().toUpperCase();
                geoBorderLinks.add(lnk);
                if (!topoBorderLinks.contains(lnk)) {
                    violations.add(geomFile + ": border-coordinates/link=" + spec.link() +
                        " — link not found in topology borders");
                }
            }
        }
        return geoBorderLinks;
    }

    // --- helpers for controls placements ---

    private static void checkStringCodePlacements(List<ElementPlacement<String>> placements,
        Set<String> validLocations, List<String> violations, String file, String section, String what)
    {
        checkItemCodes(placements, ElementPlacement::code, validLocations, violations, file, section,
            "code", " — " + what + " not found in topology");
    }

    private static void checkHydrantPlacementCodes(List<HydrantPlacement> placements,
        Set<String> validLocations, List<String> violations, String file, String section)
    {
        checkItemCodes(placements, HydrantPlacement::code, validLocations, violations, file, section,
            "code", LOCATION_NOT_FOUND_IN_TOPOLOGY);
    }

    private static void checkDoorButtonPlacements(List<ElementPlacement<String>> placements,
        Set<String> validLocations, List<String> violations, String file, String section)
    {
        if (placements == null)
            return;
        for (ElementPlacement<String> p : placements) {
            String code = (p != null) ? p.code() : null;
            if (code == null || code.length() != 2) {
                violations.add(file + ": " + section + "/code=" + code + " — must be 2-letter door code");
            } else {
                String from = code.substring(0, 1);
                String to = code.substring(1, 2);
                if (!validLocations.contains(from) || !validLocations.contains(to))
                    violations.add(file + ": " + section + "/code=" + code + " — endpoint location not found in topology");
            }
        }
    }

    /**
     * Shared referential check: each non-null item's extracted code must appear in {@code valid}. Group / placement
     * containers differ only in how the code is read and how the violation is phrased.
     */
    // Shared check + message scaffold; private, same-package call sites only — S107 not actionable.
    @SuppressWarnings("java:S107")
    private static <T> void checkItemCodes(List<T> items, Function<T, String> codeOf, Set<String> valid,
        List<String> violations, String file, String section, String codeField, String notFoundSuffix)
    {
        if (items == null)
            return;
        for (T item : items) {
            if (item != null) {
                String code = codeOf.apply(item);
                if (code != null && !valid.contains(code))
                    violations.add(file + ": " + section + "/" + codeField + "=" + code + notFoundSuffix);
            }
        }
    }

    private static <T> List<T> groupItems(ToggleGroupConfig<T> group) {
        return group == null ? null : group.items();
    }

    private static <T> List<T> groupItems(HydrantsGroupConfig<T> group) {
        return group == null ? null : group.items();
    }

    private static List<HydrLabelSpec> groupItems(HydrOutletLabelGroupConfig group) {
        return group == null ? null : group.items();
    }

    private static List<DoorGlyphSpec> groupItems(DoorButtonGroupConfig group) {
        return group == null ? null : group.items();
    }
}
