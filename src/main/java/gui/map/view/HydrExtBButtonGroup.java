package gui.map.view;

import config.specification.HydrantButtonGroupSpec;
import domain.HydrantOutlets;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.HydrantViewData;
import gui.map.view.controls.HydrExtBorderToButton;
import gui.map.view.controls.HydrantToggleButton;

import javax.swing.JLabel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class HydrExtBButtonGroup extends HydrantButtonGroup {
    public HydrExtBButtonGroup(HydrantButtonGroupSpec specification, TopologyModel topology, JLabel container) {
        super(specification, topology, container);
    }

    @Override
    protected Function<String, HydrantToggleButton> getButtonCreator() {
        return HydrExtBorderToButton::new;
    }

    @Override
    protected Set<Location> getTargetLocations(HydrantViewData data) {
        return data.fireLocations();
    }

    @Override
    protected Map<Location, List<HydrantOutlets>> getHydrantsByLocation(HydrantViewData data) {
        return data.extBToByLocation();
    }
}
