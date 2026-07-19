package config.specification;

import com.fasterxml.jackson.annotation.JsonProperty;
import config.groups.GroupKey;

public record GroupLayerSpec(
    @JsonProperty("key") GroupKey key,
    @JsonProperty("initialVisibility")  boolean initialVisibility
) {}
