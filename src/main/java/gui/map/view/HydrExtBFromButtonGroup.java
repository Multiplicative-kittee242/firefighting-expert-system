package gui.map.view;

import config.specification.HydrantButtonGroupSpec;
import domain.HydrantOutlets;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.HydrantViewData;
import gui.map.view.controls.HydrExtBorderFromButton;
import gui.map.view.controls.HydrantToggleButton;

import javax.swing.JLabel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class HydrExtBFromButtonGroup extends HydrantButtonGroup {
    public HydrExtBFromButtonGroup(HydrantButtonGroupSpec specification, TopologyModel topology, JLabel container) {
        super(specification, topology, container);
    }

    @Override
    protected Function<String, HydrantToggleButton> getButtonCreator() {
        return HydrExtBorderFromButton::new;
    }

    @Override
    protected Set<Location> getTargetLocations(HydrantViewData data) {
        return data.graphFromLocations();
    }

    @Override
    protected Map<Location, List<HydrantOutlets>> getHydrantsByLocation(HydrantViewData data) {
        return data.extBFromByLocation();
    }
}
