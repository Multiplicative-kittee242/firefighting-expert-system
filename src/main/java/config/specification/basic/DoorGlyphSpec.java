package config.specification.basic;

import com.fasterxml.jackson.annotation.JsonProperty;
import config.enums.DoorDirection;
import config.enums.DoorOrientation;
import config.enums.DoorSide;

/**
 * The render shape of a door button (orientation / alignment of the sealing glyph), keyed by door code. Split out of
 * the door's {@code position} (see {@code controls.yaml}'s {@code door-buttons-placing}, an
 * {@code ElementPlacement<String>} list like every other {@code *-placing} entry there) since this is button-appearance
 * data, not placement — merged back together by {@code gui.map.input.DoorSealingButtonGroup}.
 * <p>
 * Size is not carried here: it is one of {@code config.groups.DoorButtonGroupConfig}'s two group-level sizes (
 * {@code standard}/{@code fireRated}), picked per door by {@code gui.map.input.DoorSealingButtonGroup} based on whether
 * the door is fire-rated (derived from {@code topology.yaml}'s {@code doors}, not duplicated here) and swapped for
 * {@code orientation}.
 */
public record DoorGlyphSpec(
    @JsonProperty("code")        String doorCode,
    @JsonProperty("orientation") DoorOrientation orientation,
    @JsonProperty("align") DoorSide side,
    @JsonProperty("valign") DoorDirection direction
) {}
