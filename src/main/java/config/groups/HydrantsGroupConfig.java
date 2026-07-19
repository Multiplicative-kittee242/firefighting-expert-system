package config.groups;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record HydrantsGroupConfig<T>(
    @JsonProperty("items") List<T> items,
    @JsonProperty("widthFull") int widthFull,
    @JsonProperty("widthShort") int widthShort,
    @JsonProperty("height") int height
) {}
