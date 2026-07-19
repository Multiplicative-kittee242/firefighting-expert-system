package config.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import config.specification.basic.HydrLabelSpec;

import java.util.List;

public record HydrOutletLabelGroupConfig(
    @JsonProperty("items") List<HydrLabelSpec> items,
    @JsonProperty("baseWidth") int baseWidth,
    @JsonProperty("widthAdd") int widthAdd,
    @JsonProperty("height") int height
) {}
