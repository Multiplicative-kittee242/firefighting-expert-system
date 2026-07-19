package config.loading;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import config.YamlConfigLoader;
import config.specification.basic.BorderCoordinateSpec;
import config.specification.basic.LocationCoordinateSpec;

import java.util.List;

/**
 * Raw map-rendering geometry: compartment outlines and bulkhead outlines. Now uses explicit codes / links for each
 * geometry entry (instead of positional indexing against topology). This makes the correspondence between topology and
 * geometry data explicit and verifiable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeckMapGeometryConfig(
    @JsonProperty("location-coordinates") List<LocationCoordinateSpec> locationCoordinates,
    @JsonProperty("border-coordinates")   List<BorderCoordinateSpec> borderCoordinates
) {
    public static DeckMapGeometryConfig createDefault() {
        return YamlConfigLoader.load("config/geometry.yaml", DeckMapGeometryConfig.class);
    }
}
