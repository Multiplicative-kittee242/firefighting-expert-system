package gui.map.view;

import config.specification.basic.HydrantPlacement;
import geometry.Point;
import config.specification.HydrantButtonGroupSpec;
import domain.HydrantOutlets;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.Visible;
import gui.map.state.HydrantViewData;
import gui.map.state.HydrantViewListener;
import gui.map.view.controls.HydrantToggleButton;
import util.VisibleForTesting;

import javax.swing.JLabel;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public abstract class HydrantButtonGroup implements HydrantViewListener, Visible {
    public static final String BUTTON_TITLE_FORMAT = "hd_%s";

    private final List<HydrantToggleButton> buttons = new ArrayList<>();
    private final HydrantOffsetState offsetState = new HydrantOffsetState();
    private final Set<HydrantOutlets> hydrantOutlets;
    private final Map<Location, HydrantPlacement> hydrantPlacements;
    private final boolean variableWidth;
    private final JLabel container;

    protected HydrantButtonGroup(HydrantButtonGroupSpec specification, TopologyModel topology, JLabel container) {
        this.hydrantOutlets = new HashSet<>(specification.hydrantOutlets());
        this.hydrantPlacements = specification.configs().stream().collect(toMap(p -> topology.location(p.code()), p -> p));
        this.variableWidth = specification.variableWidth();
        this.container = container;
    }

    @Override
    public void onHydrantViewDataChanged(HydrantViewData data) {
        createButtons(data);
    }

    @Override
    public void hide() {
        for (HydrantToggleButton button : buttons) {
            if (button != null)
                button.setVisible(false);
        }
    }

    @Override
    public void show() {
        showIfEnabled();
    }

    @Override
    public void showIfEnabled() {
        for (HydrantToggleButton button : buttons) {
            if (button != null && button.isEnabled())
                button.setVisible(true);
        }
    }

    /**
     * Rebuilt on every {@link #onHydrantViewDataChanged} call (unlike {@code gui.map.input}
     * button groups, whose controls are created once and persist) — no domain-key lookup exists
     * here (there was never a stable identity to key by across rebuilds), so this exposes the flat
     * list rather than a {@code getControlFor}-style accessor.
     */
    @VisibleForTesting
    public List<HydrantToggleButton> getButtons() {
        return List.copyOf(buttons);
    }

    private void createButtons(HydrantViewData data) {
        List<HydrantToggleButton> oldButtons = resetState();
        for (HydrantToggleButton b : oldButtons)
            container.remove(b);

        Set<Location> locations = getTargetLocations(data);
        Map<Location, List<HydrantOutlets>> byLocation = getHydrantsByLocation(data);
        for (Location location : locations) {
            List<HydrantOutlets> titles = byLocation.get(location);
            if (titles != null) {
                for (HydrantToggleButton b : createVisibleButtons(titles, location, getButtonCreator()))
                    container.add(b);
            }
        }
    }

    protected abstract Function<String, HydrantToggleButton> getButtonCreator();
    protected abstract Set<Location> getTargetLocations(HydrantViewData data);
    protected abstract Map<Location, List<HydrantOutlets>> getHydrantsByLocation(HydrantViewData data);

    private List<HydrantToggleButton> resetState() {
        List<HydrantToggleButton> result = new ArrayList<>(buttons);
        buttons.clear();
        offsetState.reset();
        return result;
    }

    private List<HydrantToggleButton> createVisibleButtons(List<HydrantOutlets> targetTitles, Location location,
        Function<String, HydrantToggleButton> buttonCreator)
    {
        List<HydrantToggleButton> created = new ArrayList<>();
        for (HydrantOutlets title : targetTitles) {
            if (hydrantOutlets.contains(title)) {
                HydrantToggleButton button = buttonCreator.apply(title.getTitle());
                button.setVisible(true);
                button.setEnabled(true);
                positionButton(button, location);
                buttons.add(button);
                created.add(button);
            }
        }
        return created;
    }

    private void positionButton(HydrantToggleButton button, Location targetLocation) {
        HydrantPlacement hydrantPlacement = hydrantPlacements.get(targetLocation);
        if (hydrantPlacement == null)
            return;

        int currentOffset = offsetState.getOffset(targetLocation);
        int buttonSizeType = hydrantPlacement.count();

        int width = resolveButtonWidth(variableWidth, buttonSizeType);
        int height = buttonSizeType == 1 ? 15 : 26;

        Point position = hydrantPlacement.position();
        button.setBounds(position.x(), position.y() + currentOffset * width, 66 / buttonSizeType + 1, height);
        button.setSize(buttonSizeType);

        offsetState.setOffset(targetLocation, currentOffset + buttonSizeType);
    }

    private static int resolveButtonWidth(boolean variableWidth, int buttonSizeType) {
        if (!variableWidth)
            return 14;
        if (buttonSizeType == 1)
            return 14;
        return 12;
    }
}
