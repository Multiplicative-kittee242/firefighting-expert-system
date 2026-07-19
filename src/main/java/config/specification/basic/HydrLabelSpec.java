package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;
import geometry.Point;

/**
 * The label's digit width is not authored here: it is derived from {@code domain.HydrantOutlets#getOutlets()} at
 * label-group construction time (see {@code gui.map.view.HydrantOutletsGroup}), since it is the same value
 * {@code topology.yaml}'s {@code hydrant-outlets} already carries — authoring it a second time here risked silent drift
 * between the two.
 */
public record HydrLabelSpec(
    @JsonProperty("titleCode") String titleCode,
    @JsonProperty("position") Point position
) {}
