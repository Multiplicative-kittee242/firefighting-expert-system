package gui.map.view;

import config.specification.HydrantButtonGroupSpec;
import domain.HydrantOutlets;
import domain.Location;
import domain.registry.TopologyModel;
import gui.map.state.HydrantViewData;
import gui.map.view.controls.FireHoseButton;
import gui.map.view.controls.HydrantToggleButton;

import javax.swing.JLabel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * One toggle button per hydrant outlet CLIPS actually allocated to a fire-line location's defense
 * (one button per {@code fireLineHydrantOutletsByLocation} entry — see {@link
 * clips.ClipsEngineAccess#getFireLineHydrantsPresent} for why this count always equals {@code
 * FrontlineHydrantsBalance#here()}, never {@code need()}: there is no concrete hydrant identity for
 * a still-outstanding requirement to put a button on). The only one of the four hydrant button
 * groups this project actually populates in practice — {@link HydrExtButtonGroup}, {@link
 * HydrExtBButtonGroup} and {@link HydrExtBFromButtonGroup} are fed CLIPS fields that are empty
 * across every known scenario.
 * <p>
 * <b>Dormant feature:</b> {@code DeckMapController} never wires an {@code ActionListener}/{@code
 * InputAction} to these buttons (unlike every {@code gui.map.input} button group) — clicking one
 * today just toggles its own Swing selected state (red/green repaint) and nothing else; no
 * downstream method call, no CLIPS report. The intended meaning, per the domain semantics above:
 * confirmation by the operator that a specific allocated hydrant's hose has actually been run out
 * and put into action by the crew — CLIPS's {@code hydrants-here} only reflects the routing
 * algorithm's own (virtual) allocation, not physical completion. Not implemented yet.
 */
public class FireHoseButtonGroup extends HydrantButtonGroup {
    public FireHoseButtonGroup(HydrantButtonGroupSpec specification, TopologyModel topology, JLabel container) {
        super(specification, topology, container);
    }

    @Override
    protected Function<String, HydrantToggleButton> getButtonCreator() {
        return FireHoseButton::new;
    }

    @Override
    protected Set<Location> getTargetLocations(HydrantViewData data) {
        return data.fireLineLocations();
    }

    @Override
    protected Map<Location, List<HydrantOutlets>> getHydrantsByLocation(HydrantViewData data) {
        return data.fireLineHydrantOutletsByLocation();
    }
}
