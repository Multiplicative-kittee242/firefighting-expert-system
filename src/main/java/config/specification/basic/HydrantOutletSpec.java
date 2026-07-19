package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HydrantOutletSpec(
    @JsonProperty("title")   String title,
    @JsonProperty("outlets") int outlets
) {}
