package gui.map.view;

import config.groups.HydrantsGroupConfig;
import config.specification.ElementPlacement;
import config.specification.FirefightingStepGroupSpec;
import domain.FirefightingStep;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.HydrantViewData;
import config.enums.HydrantLabelSize;
import gui.map.view.controls.FirefightingStepLabel;

public class FirefightingStepGroup extends AbstractHydrantLabelGroup<FirefightingStepLabel, Location> {
    private final HydrantsGroupConfig<FirefightingStepGroupSpec> config;

    public FirefightingStepGroup(HydrantsGroupConfig<FirefightingStepGroupSpec> config, TopologyModel topology) {
        super(config.items().stream()
                .map(s -> new ElementPlacement<>(topology.location(s.locationCode()), s.position()))
                .toList(),
            config.items().stream()
                .map(s -> new FirefightingStepLabel("0", "0", s.size()))
                .toList()
        );
        this.config = config;
    }

    @Override
    public void onHydrantViewDataChanged(HydrantViewData data) {
        resetAll();
        for (Location location : data.fireLocations()) {
            FirefightingStep plan = data.firefightingPlans().get(location);
            if (plan != null)
                putData(location, plan.from().getCode(), String.valueOf(plan.stepNumber()));
        }
    }

    @Override
    protected int getControlWidth(FirefightingStepLabel control) {
        return control.getLabelSize() == HydrantLabelSize.FULL ? config.widthFull() : config.widthShort();
    }

    @Override
    protected int getControlHeight(FirefightingStepLabel control) {
        return config.height();
    }

    @Override
    protected void updateLabel(FirefightingStepLabel label, String from, String number) {
        label.setLabels(from, number);
    }
}
