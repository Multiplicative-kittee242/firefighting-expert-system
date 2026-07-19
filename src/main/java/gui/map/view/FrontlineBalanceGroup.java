package gui.map.view;

import config.groups.HydrantsGroupConfig;
import config.specification.ElementPlacement;
import config.specification.FrontlineBalanceGroupSpec;
import domain.FrontlineHydrantsBalance;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.HydrantViewData;
import config.enums.HydrantLabelSize;
import gui.map.view.controls.FrontlineBalanceLabel;

/**
 * Frontline hydrant hoses balance label: present / needed.
 */
public class FrontlineBalanceGroup extends AbstractHydrantLabelGroup<FrontlineBalanceLabel, Location> {
    private final HydrantsGroupConfig<FrontlineBalanceGroupSpec> config;

    public FrontlineBalanceGroup(HydrantsGroupConfig<FrontlineBalanceGroupSpec> config, TopologyModel topology) {
        super(config.items().stream()
                .map(s -> new ElementPlacement<>(topology.location(s.locationCode()), s.position()))
                .toList(),
            config.items().stream()
                .map(s -> new FrontlineBalanceLabel(0, 0, s.size()))
                .toList()
        );
        this.config = config;
    }

    @Override
    public void onHydrantViewDataChanged(HydrantViewData data) {
        resetAll();
        for (Location location : data.fireLineLocations()) {
            FrontlineHydrantsBalance hydrants = data.frontlineHydrants().get(location);
            putData(location, String.valueOf(hydrants.here()), String.valueOf(hydrants.need()));
        }
    }

    @Override
    protected int getControlWidth(FrontlineBalanceLabel control) {
        return control.getLabelSize() == HydrantLabelSize.FULL ? config.widthFull() : config.widthShort();
    }

    @Override
    protected int getControlHeight(FrontlineBalanceLabel control) {
        return config.height();
    }

    @Override
    protected void updateLabel(FrontlineBalanceLabel label, String hereString, String needString) {
        int here = Integer.parseInt(hereString.trim());
        int need = Integer.parseInt(needString.trim());
        label.setNumbers(here, need);
    }
}
