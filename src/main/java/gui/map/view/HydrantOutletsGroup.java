package gui.map.view;

import config.groups.HydrOutletLabelGroupConfig;
import config.specification.ElementPlacement;
import domain.HydrantState;
import domain.HydrantOutlets;
import domain.registry.TopologyModel;
import gui.map.state.HydrantViewData;
import gui.map.view.controls.HydrantOutletsLabel;

/**
 * Hydrant outlets: free / total.
 */
public class HydrantOutletsGroup extends AbstractHydrantLabelGroup<HydrantOutletsLabel, HydrantOutlets> {
    private final HydrOutletLabelGroupConfig config;

    public HydrantOutletsGroup(HydrOutletLabelGroupConfig config, TopologyModel topology) {
        super(config.items().stream()
                .map(s -> new ElementPlacement<>(topology.hydrantOutlets(s.titleCode()), s.position()))
                .toList(),
            config.items().stream()
                .map(s -> topology.hydrantOutlets(s.titleCode()).getOutlets())
                .map(outlets -> new HydrantOutletsLabel(outlets, outlets))
                .toList()
        );
        this.config = config;
    }

    @Override
    public void onHydrantViewDataChanged(HydrantViewData data) {
        for (HydrantState hydrantState : data.hydrantStates().values())
            putData(hydrantState.hydrant(), String.valueOf(hydrantState.currentFree()), null);
    }

    @Override
    protected int getControlWidth(HydrantOutletsLabel control) {
        return control.getLabelSize() * config.baseWidth() + config.widthAdd();
    }

    @Override
    protected int getControlHeight(HydrantOutletsLabel control) {
        return config.height();
    }

    @Override
    protected void updateLabel(HydrantOutletsLabel label, String free, String title) {
        label.setNumbers(free, title);
    }
}
