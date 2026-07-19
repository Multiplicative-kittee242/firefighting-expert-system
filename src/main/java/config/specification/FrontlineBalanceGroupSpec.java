package config.specification;

import com.fasterxml.jackson.annotation.JsonProperty;
import config.enums.HydrantLabelSize;
import geometry.Point;

public record FrontlineBalanceGroupSpec(
    @JsonProperty("locationCode") String locationCode,
    @JsonProperty("position")     Point position,
    @JsonProperty("size") HydrantLabelSize size
) implements LocationAttached {}
