package config;

import config.loading.*;
import config.validation.ConfigIntegrityChecker;
import domain.registry.TopologyModel;
import domain.types.CompartmentType;
import domain.types.ExtinguisherType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * End-to-end guard: ensures that the five real shipped config files in src/main/resources/config
 * are always valid according to the current schema and cross-file integrity rules.
 * <p>
 * This test runs on any JVM (no 32-bit JRE or CLIPS required) and will catch structural or
 * referential regressions in CI before a live run.
 */
class ShippedConfigValidityTest {

    /**
     * Single named fixture holding all five (resource, DTO) pairs.
     * Kept as a plain list of Object[] for @MethodSource compatibility and explicitness (DAMP).
     */
    private static final List<Object[]> SHIPPED_CONFIGS = List.of(
        new Object[] { "config/topology.yaml",  DeckMapTopologyConfig.class },
        new Object[] { "config/controls.yaml",  DeckMapControlsConfig.class },
        new Object[] { "config/groups.yaml",    DeckMapGroupsConfig.class },
        new Object[] { "config/assembly.yaml",  DeckMapAssemblyConfig.class },
        new Object[] { "config/geometry.yaml",  DeckMapGeometryConfig.class }
    );

    static List<Object[]> YamlConfigLoader_load() {
        return SHIPPED_CONFIGS;
    }

    @ParameterizedTest(name = "loads without error: {0}")
    @MethodSource
    void YamlConfigLoader_load(String resource, Class<?> type) {
        assertDoesNotThrow(() -> YamlConfigLoader.load(resource, type),
            () -> "Shipped config should load and validate: " + resource);
    }

    @Test
    void ConfigIntegrityChecker_check() {
        DeckMapTopologyConfig topology = DeckMapTopologyConfig.createDefault();
        DeckMapControlsConfig controls = DeckMapControlsConfig.createDefault();
        DeckMapGroupsConfig groups = DeckMapGroupsConfig.createDefault(controls);
        DeckMapGeometryConfig geometry = DeckMapGeometryConfig.createDefault();

        assertDoesNotThrow(() -> ConfigIntegrityChecker.check(topology, controls, groups, geometry),
            "Integrity check must pass for the real shipped topology/controls/groups/geometry configs");
    }

    @Test
    void DeckMapAssemblyConfig_createDefault() {
        assertDoesNotThrow(() -> {
            DeckMapConfig deckMapConfig = DeckMapConfig.createDefault();
            TopologyModel topologyModel = deckMapConfig.getTopologyConfig().buildTopologyModel();
            DeckMapAssemblyConfig.createDefault(deckMapConfig.getGroupsConfig(), topologyModel.allHydrantOutlets());
        }, "Shipped config must load, validate, resolve into the domain model and enrich assembly without error");
    }

    @Test
    void DeckMapTopologyConfig_buildTopologyModel() {
        TopologyModel model = DeckMapTopologyConfig.createDefault().buildTopologyModel();

        assertThat(model.location("E").getType(), is(CompartmentType.AUXILIARY));      // yaml "auxiliary" (clips "auxilary")
        assertThat(model.location("J").getType(), is(CompartmentType.ENGINE_ROOM));    // yaml "engine_room" (clips "engine-room")
        assertThat(model.extinguisher("est_b3").getType(), is(ExtinguisherType.AIR_FOAM));       // yaml "air_foam" (clips "af")
        assertThat(model.extinguisher("est_a").getType(), is(ExtinguisherType.CARBON_DIOXIDE));  // yaml "carbon_dioxide" (clips "co")
    }
}
