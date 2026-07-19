package config.loading;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import config.YamlConfigLoader;
import config.specification.ElementPlacement;
import config.specification.basic.HydrantPlacement;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeckMapControlsConfig(
    @JsonProperty("explosion-markers-placing")  List<ElementPlacement<String>> explosionPreventionMarkers,
    @JsonProperty("fire-buttons-placing")       List<ElementPlacement<String>> fireButtons,
    @JsonProperty("evacuation-buttons-placing") List<ElementPlacement<String>> evacuationButtons,
    @JsonProperty("door-buttons-placing")       List<ElementPlacement<String>> doorButtons,
    @JsonProperty("hydrant-placing")            List<HydrantPlacement> hydrantPlacement
) {
    public static DeckMapControlsConfig createDefault() {
        return YamlConfigLoader.load("config/controls.yaml", DeckMapControlsConfig.class);
    }
}
