package gui.map.state;

import domain.FirefightingStep;
import domain.FrontlineHydrantsBalance;
import domain.HydrantState;
import domain.HydrantOutlets;
import domain.Location;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record HydrantViewData(
    Map<Location, FrontlineHydrantsBalance> frontlineHydrants,
    Map<HydrantOutlets, HydrantState> hydrantStates,
    Map<Location, FirefightingStep> firefightingPlans,
    Set<Location> fireLineLocations,
    Map<Location, List<HydrantOutlets>> fireLineHydrantOutletsByLocation,
    Set<Location> fireLocations,
    Map<Location, List<HydrantOutlets>> extByLocation,
    Map<Location, List<HydrantOutlets>> extBToByLocation,
    Set<Location> graphFromLocations,
    Map<Location, List<HydrantOutlets>> extBFromByLocation
) {
    public static final HydrantViewData EMPTY = new HydrantViewData(Map.of(), Map.of(), Map.of(), Set.of(), Map.of(), Set.of(), Map.of(), Map.of(), Set.of(), Map.of());
}
