package gui.map.state;

import domain.Location;

import java.util.*;

public class InputExplosionsData {
    private final Set<Location> explosionThreatLocations = new LinkedHashSet<>();
    private final Set<Location> preventedExplosionLocations = new LinkedHashSet<>();

    public void updateFrom(Set<Location> newExplosionThreatLocations) {
        replaceData(explosionThreatLocations, newExplosionThreatLocations);
    }

    public void setPreventedExplosionLocations(Set<Location> locations) {
        replaceData(preventedExplosionLocations, locations);
    }

    public Set<Location> getExplosionThreatLocations() {
        return Collections.unmodifiableSet(explosionThreatLocations);
    }

    public Set<Location> getPreventedExplosionLocations() {
        return Collections.unmodifiableSet(preventedExplosionLocations);
    }

    public Set<Location> fetchPendingExplosionPreventionLocations() {
        Set<Location> pending = new LinkedHashSet<>(explosionThreatLocations);
        pending.removeAll(preventedExplosionLocations);
        return Collections.unmodifiableSet(pending);
    }

    private <T> void replaceData(Set<T> target, Collection<T> source) {
        target.clear();
        target.addAll(source);
    }
}
