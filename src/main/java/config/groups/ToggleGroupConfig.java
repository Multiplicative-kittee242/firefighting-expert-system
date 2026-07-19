package config.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import geometry.Size;

import java.util.List;

public record ToggleGroupConfig<T>(
    @JsonProperty("items") List<T> items,
    @JsonProperty("size") Size size
) {}
