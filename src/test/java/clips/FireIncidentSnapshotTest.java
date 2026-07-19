package clips;

import domain.FirefightingStep;
import domain.FrontlineHydrantsBalance;
import domain.HydrantOutlets;
import domain.HydrantState;
import domain.Link;
import domain.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FireIncidentSnapshotTest {
    private static final Location ROOM_A = new Location("A");
    private static final Location ROOM_B = new Location("B");
    private static final Link LINK_AB = new Link("AB", ROOM_A, ROOM_B);
    private static final HydrantOutlets HYDR_F = new HydrantOutlets("hydr_f", ROOM_A, 1);

    @Test
    void nullCollections_BecomeEmptyImmutableViews() {
        FireIncidentSnapshot snapshot = emptyViaNulls();

        assertThat(snapshot.fireLocations(), is(Set.of()));
        assertThat(snapshot.sealingDoorsToClose(), is(List.of()));
        assertThat(snapshot.frontlineHydrantsBalance(), is(Map.of()));
        assertThat(snapshot.hydrantOutletsState(), is(Map.of()));
        assertThat(snapshot.extByLocation(), is(Map.of()));
    }

    @Test
    void emptyCollections_BecomeEmptyImmutableViews() {
        FireIncidentSnapshot snapshot = emptyViaEmpties();

        assertThat(snapshot.fireLocations(), is(Set.of()));
        assertThat(snapshot.sealingDoorsToClose(), is(List.of()));
        assertThat(snapshot.frontlineHydrantsBalance(), is(Map.of()));
        assertThat(snapshot.hydrantOutletsState(), is(Map.of()));
        assertThat(snapshot.extByLocation(), is(Map.of()));
    }

    @Test
    void returnedViews_AreUnmodifiable() {
        FireIncidentSnapshot snapshot = populated();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.fireLocations().add(ROOM_A));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.sealingDoorsToClose().add(LINK_AB));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.frontlineHydrantsBalance().put(ROOM_A,
            new FrontlineHydrantsBalance(0, 1)));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.hydrantOutletsState().put(HYDR_F,
            new HydrantState(HYDR_F, 1, 0)));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.extByLocation().put(ROOM_A, List.of()));
        assertThrows(UnsupportedOperationException.class,
            () -> snapshot.extByLocation().get(ROOM_A).add(HYDR_F));
    }

    @Test
    void nonEmptyInputs_ReachGettersUnchanged() {
        FireIncidentSnapshot snapshot = populated();

        assertThat(snapshot.fireLocations(), containsInAnyOrder(ROOM_A));
        assertThat(snapshot.threatenedLocations(), containsInAnyOrder(ROOM_B));
        assertThat(snapshot.evacuationLocations(), containsInAnyOrder(ROOM_B));
        assertThat(snapshot.ventilationOffLocations(), containsInAnyOrder(ROOM_A));
        assertThat(snapshot.sealingDoorsToClose(), contains(LINK_AB));
        assertThat(snapshot.sealingDoorsKeepOpen(), empty());
        assertThat(snapshot.explosionThreatLocations(), containsInAnyOrder(ROOM_A));
        assertThat(snapshot.flammableLocations(), containsInAnyOrder(ROOM_A));
        assertThat(snapshot.machineryDamageLocations(), containsInAnyOrder(ROOM_A));
        assertThat(snapshot.fireLineLinks(), contains(LINK_AB));
        assertThat(snapshot.fireLineLocations(), containsInAnyOrder(ROOM_B));
        assertThat(snapshot.graphFromLocations(), containsInAnyOrder(ROOM_A));
        assertThat(snapshot.frontlineHydrantsBalance().get(ROOM_B), is(new FrontlineHydrantsBalance(1, 2)));
        assertThat(snapshot.hydrantOutletsState().get(HYDR_F), is(new HydrantState(HYDR_F, 1, 1)));
        assertThat(snapshot.firefightingPlanSteps().get(ROOM_A), is(new FirefightingStep(ROOM_B, 1)));
        assertThat(snapshot.extByLocation().get(ROOM_A), contains(HYDR_F));
        assertThat(snapshot.extBToByLocation().get(ROOM_B), contains(HYDR_F));
        assertThat(snapshot.extBFromByLocation().get(ROOM_A), contains(HYDR_F));
        assertThat(snapshot.fireLineHydrantOutletsByLocation().get(ROOM_B), contains(HYDR_F));
    }

    @Test
    void constructor_DefensivelyCopiesMutableInputs() {
        Set<Location> fires = new HashSet<>();
        fires.add(ROOM_A);
        List<Link> doors = new ArrayList<>();
        doors.add(LINK_AB);
        Map<Location, List<HydrantOutlets>> ext = new HashMap<>();
        List<HydrantOutlets> list = new ArrayList<>();
        list.add(HYDR_F);
        ext.put(ROOM_A, list);

        FireIncidentSnapshot snapshot = new FireIncidentSnapshot(
            fires, Set.of(), Set.of(), Set.of(),
            doors, List.of(),
            Set.of(), Set.of(), Set.of(),
            List.of(), Set.of(), Set.of(),
            Map.of(), Map.of(), Map.of(),
            Map.of(), ext, Map.of(), Map.of());

        fires.add(ROOM_B);
        doors.clear();
        list.clear();
        ext.clear();

        assertThat(snapshot.fireLocations(), containsInAnyOrder(ROOM_A));
        assertThat(snapshot.sealingDoorsToClose(), contains(LINK_AB));
        assertThat(snapshot.extByLocation().get(ROOM_A), contains(HYDR_F));
    }

    private static FireIncidentSnapshot emptyViaNulls() {
        return new FireIncidentSnapshot(
            null, null, null, null,
            null, null,
            null, null, null,
            null, null, null,
            null, null, null,
            null, null, null, null);
    }

    private static FireIncidentSnapshot emptyViaEmpties() {
        return new FireIncidentSnapshot(
            Set.of(), Set.of(), Set.of(), Set.of(),
            List.of(), List.of(),
            Set.of(), Set.of(), Set.of(),
            List.of(), Set.of(), Set.of(),
            Map.of(), Map.of(), Map.of(),
            Map.of(), Map.of(), Map.of(), Map.of());
    }

    private static FireIncidentSnapshot populated() {
        return new FireIncidentSnapshot(
            Set.of(ROOM_A),
            Set.of(ROOM_B),
            Set.of(ROOM_B),
            Set.of(ROOM_A),
            List.of(LINK_AB),
            List.of(),
            Set.of(ROOM_A),
            Set.of(ROOM_A),
            Set.of(ROOM_A),
            List.of(LINK_AB),
            Set.of(ROOM_B),
            Set.of(ROOM_A),
            Map.of(ROOM_B, new FrontlineHydrantsBalance(1, 2)),
            Map.of(HYDR_F, new HydrantState(HYDR_F, 1, 1)),
            Map.of(ROOM_A, new FirefightingStep(ROOM_B, 1)),
            Map.of(ROOM_B, List.of(HYDR_F)),
            Map.of(ROOM_A, List.of(HYDR_F)),
            Map.of(ROOM_A, List.of(HYDR_F)),
            Map.of(ROOM_B, List.of(HYDR_F)));
    }
}
