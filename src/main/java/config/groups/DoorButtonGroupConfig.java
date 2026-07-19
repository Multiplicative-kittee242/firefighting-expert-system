package config.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import config.specification.basic.DoorGlyphSpec;
import geometry.Size;

import java.util.List;

/**
 * The two door-button size classes ({@code standard}/{@code fireRated}, both given in their "vertical" form) plus the
 * per-door render shapes. See {@code gui.map.input.DoorSealingButtonGroup} for how a door's actual size is picked
 * (fire-rated or not) and oriented (swapped for a horisontal door).
 */
public record DoorButtonGroupConfig(
    @JsonProperty("items") List<DoorGlyphSpec> items,
    @JsonProperty("size") Sizes size
) {
    /**
     * Accessors for decomposed sizes (used by gui). Provided for backward compat of callers while the canonical /
     * record shape matches the YAML "size" structure for correct schema.
     */
    public Size standardSize() {
        return size != null ? size.standard() : null;
    }

    public Size fireRatedSize() {
        return size != null ? size.fireRated() : null;
    }

    public record Sizes(
        @JsonProperty("standard") Size standard,
        @JsonProperty("fireRated") Size fireRated
    ) {}
}
