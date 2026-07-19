package config.specification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import config.groups.GroupKey;
import config.specification.basic.HydrantPlacement;
import domain.HydrantOutlets;

import java.util.List;

public record HydrantButtonGroupSpec(GroupKey key, List<HydrantPlacement> configs, boolean variableWidth, List<HydrantOutlets> hydrantOutlets) {

    @JsonCreator
    public HydrantButtonGroupSpec(
        @JsonProperty("key")           GroupKey key,
        @JsonProperty("variableWidth") boolean variableWidth
    ) {
        this(key, List.of(), variableWidth, List.of());
    }
}
