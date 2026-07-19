package config.loading;

import config.validation.ConfigIntegrityChecker;

/**
 * Validated bundle of the four map configs loaded together at startup: topology, geometry, controls, and groups.
 * Obtained only via {@link #createDefault()}, which loads each file, schema-validates it, then runs
 * {@link ConfigIntegrityChecker} across them — there is no public way to construct an unvalidated bundle.
 * {@code assembly.yaml} is loaded separately ({@link DeckMapAssemblyConfig}) because it needs the already-built
 * {@code TopologyModel}.
 */
public class DeckMapConfig {
    private final DeckMapTopologyConfig topologyConfig;
    private final DeckMapGeometryConfig geometryConfig;
    private final DeckMapControlsConfig controlConfig;
    private final DeckMapGroupsConfig groupsConfig;

    private DeckMapConfig(DeckMapTopologyConfig topologyConfig, DeckMapGeometryConfig geometryConfig,
        DeckMapControlsConfig controlConfig, DeckMapGroupsConfig groupsConfig)
    {
        this.topologyConfig = topologyConfig;
        this.geometryConfig = geometryConfig;
        this.controlConfig = controlConfig;
        this.groupsConfig = groupsConfig;
    }

    public static DeckMapConfig createDefault() {
        DeckMapTopologyConfig topologyConfig = DeckMapTopologyConfig.createDefault();
        DeckMapGeometryConfig geometryConfig = DeckMapGeometryConfig.createDefault();
        DeckMapControlsConfig controlConfig = DeckMapControlsConfig.createDefault();
        DeckMapGroupsConfig groupsConfig = DeckMapGroupsConfig.createDefault(controlConfig);

        ConfigIntegrityChecker.check(topologyConfig, controlConfig, groupsConfig, geometryConfig);

        return new DeckMapConfig(topologyConfig, geometryConfig, controlConfig, groupsConfig);
    }

    public DeckMapTopologyConfig getTopologyConfig() {
        return topologyConfig;
    }

    public DeckMapGeometryConfig getGeometryConfig() {
        return geometryConfig;
    }

    public DeckMapControlsConfig getControlConfig() {
        return controlConfig;
    }

    public DeckMapGroupsConfig getGroupsConfig() {
        return groupsConfig;
    }
}
