package config;

import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapControlsConfig;
import config.loading.DeckMapGeometryConfig;
import config.loading.DeckMapTopologyConfig;
import config.validation.ConfigValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the unified YamlConfigLoader: success path, schema rejection of unknown keys,
 * and aggregation of multiple validation errors (not fail-fast).
 */
class YamlConfigLoaderTest {

    @Test
    void load_LoadsValidYamlSuccessfully() {
        // use real committed resource (guaranteed valid)
        DeckMapGeometryConfig config = YamlConfigLoader.load("config/geometry.yaml", DeckMapGeometryConfig.class);

        assertThat(config, is(notNullValue()));
        assertThat(config.locationCoordinates(), is(not(empty())));
        assertThat(config.borderCoordinates(), is(not(empty())));
    }

    @Test
    void loadFromYamlContent_RejectsUnknownTopLevelKey() {
        String badYaml = """
            $schema: ./schemas/controls-schema.json
            explosion-markers-placing: []
            unknown-typo-key: [ {code: "X", position: {x: 0, y: 0}} ]
            """;

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> YamlConfigLoader.loadFromYamlContent(badYaml, "config/controls.yaml", DeckMapControlsConfig.class));

        assertThat(ex.getViolations(), hasSize(greaterThanOrEqualTo(1)));
        String msg = ex.getMessage();
        assertThat(msg, containsString("config/controls.yaml"));
        assertThat(msg, containsString("unknown-typo-key"));
    }

    @Test
    void loadFromYamlContent_AggregatesAllErrorsInsteadOfFailFast() {
        // two independent unknown keys -> two violations reported together
        String badYaml = """
            $schema: ./schemas/assembly-schema.json
            group-layers: []
            first-bad-key: 1
            second-bad-key: 2
            """;

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> YamlConfigLoader.loadFromYamlContent(badYaml, "config/assembly.yaml", DeckMapAssemblyConfig.class));

        List<String> violations = ex.getViolations();
        assertThat(violations, hasSize(greaterThanOrEqualTo(2)));
        String full = ex.getMessage();
        assertThat(full, containsString("first-bad-key"));
        assertThat(full, containsString("second-bad-key"));
        // both appear in the single aggregated message
        assertThat(full, containsString("\n"));
    }

    @Test
    void loadFromYamlContent_RejectsBogusEnumValue() {
        String badYaml = """
            $schema: ./schemas/topology-schema.json
            location-labels: [
              { code: "X", tank: 3, area: 10, type: "bogus-type" }
            ]
            borders: []
            doors: []
            evacuation-routes: []
            fire-hose-spans: { "door-to-door": [], "hydrant-to-door": [] }
            fire-sensor-codes: []
            hydrant-outlets: []
            extinguishers: []
            """;

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
            () -> YamlConfigLoader.loadFromYamlContent(badYaml, "config/topology.yaml", DeckMapTopologyConfig.class));

        List<String> violations = ex.getViolations();
        assertThat(violations, hasSize(greaterThanOrEqualTo(1)));
        String full = violations.toString();
        assertThat(full, containsString("config/topology.yaml"));
        assertThat(full, containsString("/location-labels/0/type"));
    }
}
