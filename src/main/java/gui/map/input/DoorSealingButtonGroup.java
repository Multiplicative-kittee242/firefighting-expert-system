package gui.map.input;

import gui.actions.InputAction;
import clips.values.DoorState;
import config.groups.DoorButtonGroupConfig;
import config.specification.basic.DoorGlyphSpec;
import config.specification.ElementPlacement;
import domain.Link;
import domain.registry.TopologyModel;
import geometry.Size;
import gui.map.input.controls.DoorSealingButton;
import gui.map.state.InputControlListener;
import gui.map.state.InputControlsData;
import gui.map.values.DoorKeepType;
import config.enums.DoorOrientation;

import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Self-contained group that owns the door sealing buttons (dr).
 * <p>
 * Additionally, doors support the "keep-open" state (with/without hose),
 * which is managed via {@link #setKeepOpen(Link, DoorKeepType)}.
 */
public class DoorSealingButtonGroup extends AbstractToggleGroup<DoorSealingButton, Link> implements InputControlListener {
    private final Map<String, DoorGlyphSpec> glyphByCode;
    private final Size standardSize;
    private final Size fireRatedSize;

    /**
     * {@code placements} ({@code controls.yaml}'s {@code door-buttons-placing}, code + position)
     * and {@code config} ({@code groups.yaml}'s {@code door-button-group}, code + button shape)
     * are merged here by door code — split across the two files since one is a placement concern
     * and the other a render-shape concern (see both specs' javadoc).
     */
    public DoorSealingButtonGroup(List<ElementPlacement<String>> placements, DoorButtonGroupConfig config,
                                  Map<String, Boolean> fireRatedByCode, TopologyModel topology)
    {
        super(placements.stream()
                .map(p -> new ElementPlacement<>(topology.link(p.code()), p.point()))
                .toList(),
            placements.stream()
                .map(p -> toGlyphMap(config.items()).get(p.code()))
                .map(g -> new DoorSealingButton(g.doorCode(), fireRatedByCode.getOrDefault(g.doorCode(), false),
                    g.orientation(), g.side(), g.direction(), DoorKeepType.NO))
                .toList(),
            0, 0
        );
        this.glyphByCode = toGlyphMap(config.items());
        this.standardSize = config.standardSize();
        this.fireRatedSize = config.fireRatedSize();
    }

    private static Map<String, DoorGlyphSpec> toGlyphMap(List<DoorGlyphSpec> glyphSpecs) {
        return glyphSpecs.stream().collect(toMap(DoorGlyphSpec::doorCode, spec -> spec));
    }

    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((door, button) -> attachInputAction(button, new InputAction.DoorSealingActionInput(door)));
    }

    public Map<Link, DoorState> collectChanges(Link targetDoor) {
        Map<Link, DoorState> doorSealingChanges = new LinkedHashMap<>();
        DoorSealingButton control = getControlFor(targetDoor);
        if (control != null) {
            DoorState status = control.isSelected() ? DoorState.CLOSE : DoorState.OPEN;
            doorSealingChanges.put(targetDoor, status);
        }
        return doorSealingChanges;
    }

    @Override
    public void onInputControlsDataChanged(InputControlsData data) {
        for (Link door : data.sealingDoorsToClose())
            applyDoorState(door, DoorKeepType.NO);
        for (Link door : data.sealingDoorsKeepOpen())
            applyDoorState(door, DoorKeepType.YES);
    }

    private void applyDoorState(Link door, DoorKeepType no) {
        setVisibleFor(door);
        setKeepOpen(door, no);
    }

    private void setKeepOpen(Link door, DoorKeepType keepOpenType) {
        DoorSealingButton button = getControlFor(door);
        if (button != null)
            button.setKeepOpen(keepOpenType);
    }

    @Override
    protected int getControlWidth(DoorSealingButton control) {
        return resolveSize(control).width();
    }

    @Override
    protected int getControlHeight(DoorSealingButton control) {
        return resolveSize(control).height();
    }

    /**
     * {@code standardSize}/{@code fireRatedSize} are both authored in "vertical" form (see
     * {@code groups.yaml}'s {@code door-button-group}); a "horisontal" door swaps width and height.
     */
    private Size resolveSize(DoorSealingButton control) {
        Size verticalSize = control.isFireRated() ? fireRatedSize : standardSize;
        DoorOrientation orientation = glyphByCode.get(control.getDoorCode()).orientation();
        return orientation == DoorOrientation.VERTICAL
            ? verticalSize
            : new Size(verticalSize.height(), verticalSize.width());
    }
}
