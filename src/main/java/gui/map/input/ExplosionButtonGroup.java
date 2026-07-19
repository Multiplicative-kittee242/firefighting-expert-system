package gui.map.input;

import gui.actions.InputAction;
import config.groups.ToggleGroupConfig;
import config.specification.ElementPlacement;
import config.specification.buttons.ExplosionButtonSpec;
import domain.types.ExplosiveMaterial;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.ExplosionControlListener;
import gui.map.state.InputExplosionsData;
import gui.map.input.controls.ExplosionButton;
import domain.types.ExplosiveType;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExplosionButtonGroup extends AbstractToggleGroup<ExplosionButton, Location> implements ExplosionControlListener {
    private final Consumer<Set<Location>> preventedLocationsStorage;
    private final Map<Location, List<ExplosionButton>> buttonsByLocation = new LinkedHashMap<>();

    public ExplosionButtonGroup(ToggleGroupConfig<ExplosionButtonSpec> config, TopologyModel topology, Consumer<Set<Location>> preventedLocationsStorage) {
        super(config.items().stream()
                .map(s -> new ElementPlacement<>(topology.location(s.locationCode()), s.position()))
                .toList(),
            config.items().stream()
                .map(s -> new ExplosionButton(explosiveTypeFor(topology.location(s.locationCode()))))
                .toList(),
            config.size().width(),
            config.size().height()
        );
        this.preventedLocationsStorage = preventedLocationsStorage;

        forEachControl((location, button) ->
            buttonsByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(button));
    }

    private static ExplosiveType explosiveTypeFor(Location location) {
        ExplosiveMaterial material = location.getExplosiveMaterial()
            .orElseThrow(() -> new IllegalStateException("Location " + location.getCode() + " has an explosion button but no explosive material"));
        return switch (material) {
            case CHEMICAL_REAGENT -> ExplosiveType.REAGENT;
            case DIESEL_OIL -> ExplosiveType.OIL;
            case COMPRESSED_AIR -> ExplosiveType.AIR;
        };
    }

    @Override
    public void onExplosionDataChanged(InputExplosionsData data) {
        for (Location location : data.getExplosionThreatLocations())
            setVisibleFor(location);
    }

    @Override
    public void addActionListener(ActionListener listener) {
        super.addActionListener(listener);
        forEachControl((location, button) ->
            attachInputAction(button, new InputAction.ExplosionPreventionActionInput(location, button.getType())));
    }

    public Map<Location, ExplosiveType> collectChanges(Location targetLocation, ExplosiveType explosiveType) {
        Map<Location, ExplosiveType> explosionChanges = new LinkedHashMap<>();
        ExplosionButton control = getControlFor(targetLocation);
        if (control != null) {
            boolean selected = control.isSelected();
            explosionChanges.put(targetLocation, selected ? ExplosiveType.DONE : explosiveType);
        }
        preventedLocationsStorage.accept(collectPreventedLocations());
        return explosionChanges;
    }

    private Set<Location> collectPreventedLocations() {
        return buttonsByLocation.keySet().stream()
            .filter(this::isPrevented)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isPrevented(Location location) {
        List<ExplosionButton> buttons = buttonsByLocation.get(location);
        if (buttons == null || buttons.isEmpty()) {
            return true;
        } else {
            return buttons.stream().allMatch(AbstractButton::isSelected);
        }
    }
}
