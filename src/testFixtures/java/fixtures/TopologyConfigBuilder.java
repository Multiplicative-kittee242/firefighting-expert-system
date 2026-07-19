package fixtures;

import config.specification.basic.BorderSpec;
import config.loading.DeckMapTopologyConfig;
import config.specification.basic.DoorSpec;
import config.specification.basic.EvacuationRouteSpec;
import config.specification.basic.ExtinguisherSpec;
import config.specification.basic.FireHoseSpansSpec;
import config.specification.basic.FireSensorSpec;
import config.specification.basic.HydrantOutletSpec;
import config.specification.basic.LocationSpec;

import java.util.List;

/**
 * Test data builder for {@link DeckMapTopologyConfig}: every section defaults to empty, and each
 * test sets only the sections it exercises via named methods. Replaces the positional
 * {@code new DeckMapTopologyConfig(List.of(), List.of(), doors, List.of(), …)} form, where the
 * eight same-typed list arguments are easy to misorder and impossible to read at the call site.
 */
public final class TopologyConfigBuilder {
    private List<LocationSpec> locations = List.of();
    private List<BorderSpec> borders = List.of();
    private List<DoorSpec> doors = List.of();
    private List<EvacuationRouteSpec> evacuationRoutes = List.of();
    private FireHoseSpansSpec fireHoseSpans = new FireHoseSpansSpec(List.of(), List.of());
    private List<FireSensorSpec> sensors = List.of();
    private List<HydrantOutletSpec> hydrants = List.of();
    private List<ExtinguisherSpec> extinguishers = List.of();

    public static TopologyConfigBuilder topologyConfig() {
        return new TopologyConfigBuilder();
    }

    public TopologyConfigBuilder locations(LocationSpec... locations) {
        this.locations = List.of(locations);
        return this;
    }

    public TopologyConfigBuilder borders(BorderSpec... borders) {
        this.borders = List.of(borders);
        return this;
    }

    public TopologyConfigBuilder doors(DoorSpec... doors) {
        this.doors = List.of(doors);
        return this;
    }

    public TopologyConfigBuilder evacuationRoutes(EvacuationRouteSpec... evacuationRoutes) {
        this.evacuationRoutes = List.of(evacuationRoutes);
        return this;
    }

    public TopologyConfigBuilder fireHoseSpans(FireHoseSpansSpec fireHoseSpans) {
        this.fireHoseSpans = fireHoseSpans;
        return this;
    }

    public TopologyConfigBuilder sensors(FireSensorSpec... sensors) {
        this.sensors = List.of(sensors);
        return this;
    }

    public TopologyConfigBuilder hydrants(HydrantOutletSpec... hydrants) {
        this.hydrants = List.of(hydrants);
        return this;
    }

    public TopologyConfigBuilder extinguishers(ExtinguisherSpec... extinguishers) {
        this.extinguishers = List.of(extinguishers);
        return this;
    }

    public DeckMapTopologyConfig build() {
        return new DeckMapTopologyConfig(locations, borders, doors, evacuationRoutes,
            fireHoseSpans, sensors, hydrants, extinguishers);
    }
}
