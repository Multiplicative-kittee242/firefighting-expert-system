package gui.map.state;

import domain.Link;
import domain.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FireIncidentState {
    private final InputExplosionsData inputExplosionsData = new InputExplosionsData();

    private final List<MapDrawingListener> mapDrawingListeners = new ArrayList<>();
    private final List<HydrantViewListener> hydrantViewListeners = new ArrayList<>();
    private final List<InputControlListener> inputControlListeners = new ArrayList<>();
    private final List<ExplosionControlListener> explosionListeners = new ArrayList<>();

    private PaintingViewData paintingViewData;
    private HydrantViewData hydrantViewData;
    private InputControlsData inputControlsData;

    public FireIncidentState() {
        this.paintingViewData = PaintingViewData.EMPTY;
        this.hydrantViewData = HydrantViewData.EMPTY;
        this.inputControlsData = InputControlsData.EMPTY;
    }

    public void addMapDrawingListener(MapDrawingListener listener) {
        mapDrawingListeners.add(listener);
    }

    public void addHydrantViewListener(HydrantViewListener listener) {
        hydrantViewListeners.add(listener);
    }

    public void addInputControlListener(InputControlListener listener) {
        inputControlListeners.add(listener);
    }

    public void addExplosionControlListener(ExplosionControlListener listener) {
        explosionListeners.add(listener);
    }

    public void updateState(PaintingViewData paintingViewData, HydrantViewData hydrantViewData,
        InputControlsData inputControlsData, Set<Location> explosionThreatLocations)
    {
        this.paintingViewData = paintingViewData;
        this.hydrantViewData = hydrantViewData;
        this.inputControlsData = inputControlsData;
        inputExplosionsData.updateFrom(explosionThreatLocations);

        notifyInputControlListeners();
        notifyExplosionListeners();
        notifyHydrantListeners();
        notifyMapDrawingListeners();
    }

    private void notifyMapDrawingListeners() {
        for (MapDrawingListener l : mapDrawingListeners)
            l.onMapDrawingDataChanged(paintingViewData);
    }

    private void notifyHydrantListeners() {
        for (HydrantViewListener l : hydrantViewListeners)
            l.onHydrantViewDataChanged(hydrantViewData);
    }

    private void notifyInputControlListeners() {
        for (InputControlListener l : inputControlListeners)
            l.onInputControlsDataChanged(inputControlsData);
    }

    private void notifyExplosionListeners() {
        for (ExplosionControlListener l : explosionListeners)
            l.onExplosionDataChanged(inputExplosionsData);
    }

    public Set<Location> getEvacuationLocations() {
        return inputControlsData.evacuationLocations();
    }

    //================================================================
    // Explosion related input data
    //================================================================

    /**
     * Stores the current set of locations that are explosion threats according to CLIPS.
     * This data comes from the expert system and is used both for showing prevention buttons
     * and for calculating which explosion markers should still be displayed on the map.
     */
    public void setExplosionThreatLocations(Set<Location> locations) {
        inputExplosionsData.updateFrom(locations);
        notifyExplosionListeners();
        notifyMapDrawingListeners();
    }

    /**
     * Stores the set of locations where the user has already prevented explosions.
     * This state is owned by ExplosionButtonGroup and pushed here when the user interacts with the buttons.
     * It is NOT automatically cleared when the set of threats changes — it represents user actions.
     */
    public void setPreventedExplosionLocations(Set<Location> locations) {
        inputExplosionsData.setPreventedExplosionLocations(locations);
    }

    public Set<Location> fetchPendingExplosionPreventionLocations() {
        return inputExplosionsData.fetchPendingExplosionPreventionLocations();
    }

    public Set<Location> getPreventedExplosionLocations() {
        return inputExplosionsData.getPreventedExplosionLocations();
    }

    //================================================================
    // Painting related data
    //================================================================

    public Set<Location> getFireLocations() {
        return paintingViewData.fireLocations();
    }

    public Set<Location> getThreatenedLocations() {
        return paintingViewData.threatenedLocations();
    }

    public List<Link> getFireLines() {
        return paintingViewData.fireLineLinks();
    }
}
