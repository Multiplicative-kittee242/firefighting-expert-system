package gui.map;

import clips.FireIncidentSnapshot;
import config.groups.GroupKey;
import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import config.specification.GroupLayerSpec;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import gui.Localization;
import gui.map.input.AbstractControlGroup;
import org.junit.jupiter.api.Test;
import util.ResourceUtil;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direction 3.3: {@code DeckMapController#representFire(FireIncidentSnapshot)} → correct render
 * state, without CLIPS (a hand-built mock snapshot). Every {@code collect*}-driven group reveals
 * exactly the buttons for the locations/doors its own incident-data field names, via {@code
 * onInputControlsDataChanged}/{@code onExplosionDataChanged} → {@code setVisibleFor} (visible +
 * enabled + deselected) — this proves that reactive wiring end to end, distinct from {@code
 * ClickThroughInputActionTest} (which starts every button already visible, by recipe, specifically
 * to exercise clicking) and from {@code DeckMapControllerSelectiveGroupConstructionTest}
 * (construction only, no incident data at all).
 * <p>
 * Each thematic controller here is built with {@code initialVisibility = false} — matching the real
 * shipped {@code assembly.yaml} for every one of these groups — specifically to observe the
 * false-to-true transition {@code representFire} is supposed to drive; {@code
 * ClickThroughInputActionTest}'s recipe (initial visibility {@code true}) would hide the exact
 * effect this class exists to prove.
 */
class RepresentFireInputControlsTest {

    @Test
    void representFire_RevealsEvacuationButtonsOnlyForReportedLocations() {
        ThematicSetup setup = buildThematicController(GroupKey.EVACUATION_GROUP);
        Map<Location, ? extends AbstractButton> buttons = collectButtons(setup.controller().inputGroups().getEvacuationGroup());
        Location reported = firstKey(buttons);
        Location other = otherKey(buttons, reported);

        FireIncidentSnapshot snapshot = mock(FireIncidentSnapshot.class);
        when(snapshot.evacuationLocations()).thenReturn(Set.of(reported));
        setup.controller().representFire(snapshot);

        assertRevealed(buttons.get(reported));
        assertNotRevealed(buttons.get(other));
    }

    @Test
    void representFire_RevealsVentilationButtonsOnlyForReportedLocations() {
        ThematicSetup setup = buildThematicController(GroupKey.VENTILATION_GROUP);
        Map<Location, ? extends AbstractButton> buttons = collectButtons(setup.controller().inputGroups().getVentilationGroup());
        Location reported = firstKey(buttons);
        Location other = otherKey(buttons, reported);

        FireIncidentSnapshot snapshot = mock(FireIncidentSnapshot.class);
        when(snapshot.ventilationOffLocations()).thenReturn(Set.of(reported));
        setup.controller().representFire(snapshot);

        assertRevealed(buttons.get(reported));
        assertNotRevealed(buttons.get(other));
    }

    @Test
    void representFire_RevealsFlammableButtonsOnlyForReportedLocations() {
        ThematicSetup setup = buildThematicController(GroupKey.FLAMMABLE_GROUP);
        Map<Location, ? extends AbstractButton> buttons = collectButtons(setup.controller().inputGroups().getFlammableGroup());
        Location reported = firstKey(buttons);
        Location other = otherKey(buttons, reported);

        FireIncidentSnapshot snapshot = mock(FireIncidentSnapshot.class);
        when(snapshot.flammableLocations()).thenReturn(Set.of(reported));
        setup.controller().representFire(snapshot);

        assertRevealed(buttons.get(reported));
        assertNotRevealed(buttons.get(other));
    }

    @Test
    void representFire_RevealsMachineryDamageButtonsOnlyForReportedLocations() {
        ThematicSetup setup = buildThematicController(GroupKey.MACHINERY_DAMAGE_GROUP);
        Map<Location, ? extends AbstractButton> buttons = collectButtons(setup.controller().inputGroups().getMachineryDamageGroup());
        Location reported = firstKey(buttons);
        Location other = otherKey(buttons, reported);

        FireIncidentSnapshot snapshot = mock(FireIncidentSnapshot.class);
        when(snapshot.machineryDamageLocations()).thenReturn(Set.of(reported));
        setup.controller().representFire(snapshot);

        assertRevealed(buttons.get(reported));
        assertNotRevealed(buttons.get(other));
    }

    @Test
    void representFire_RevealsExplosionButtonsOnlyForReportedLocations() {
        ThematicSetup setup = buildThematicController(GroupKey.EXPLOSION_GROUP);
        Map<Location, ? extends AbstractButton> buttons = collectButtons(setup.controller().inputGroups().getExplosionGroup());
        Location reported = firstKey(buttons);
        Location other = otherKey(buttons, reported);

        FireIncidentSnapshot snapshot = mock(FireIncidentSnapshot.class);
        when(snapshot.explosionThreatLocations()).thenReturn(Set.of(reported));
        setup.controller().representFire(snapshot);

        assertRevealed(buttons.get(reported));
        assertNotRevealed(buttons.get(other));
    }

    /**
     * Door sealing has two distinct incident-data channels (to-close vs. keep-open — see {@code
     * gui.map.input.DoorSealingButtonGroup#onInputControlsDataChanged}) instead of one; both reveal
     * their door's button, so a reported door from either set becomes visible.
     */
    @Test
    void representFire_RevealsDoorSealingButtonsForBothToCloseAndKeepOpenSets() {
        ThematicSetup setup = buildThematicController(GroupKey.DOOR_SEALING_GROUP);
        Map<Link, ? extends AbstractButton> buttons = collectButtons(setup.controller().inputGroups().getDoorSealingGroup());
        List<Link> doors = new ArrayList<>(buttons.keySet());
        Link toCloseDoor = doors.get(0);
        Link keepOpenDoor = doors.get(1);
        Link untouchedDoor = doors.get(2);

        FireIncidentSnapshot snapshot = mock(FireIncidentSnapshot.class);
        when(snapshot.sealingDoorsToClose()).thenReturn(List.of(toCloseDoor));
        when(snapshot.sealingDoorsKeepOpen()).thenReturn(List.of(keepOpenDoor));
        setup.controller().representFire(snapshot);

        assertRevealed(buttons.get(toCloseDoor));
        assertRevealed(buttons.get(keepOpenDoor));
        assertNotRevealed(buttons.get(untouchedDoor));
    }

    private static void assertRevealed(AbstractButton button) {
        assertThat(button.isVisible(), is(true));
        assertThat(button.isEnabled(), is(true));
    }

    private static void assertNotRevealed(AbstractButton button) {
        assertThat(button.isVisible(), is(false));
        assertThat(button.isEnabled(), is(false));
    }

    private static <D, T extends JComponent> Map<D, T> collectButtons(AbstractControlGroup<T, D> group) {
        Map<D, T> buttons = new LinkedHashMap<>();
        group.forEachControl(buttons::put);
        return buttons;
    }

    private static <D> D firstKey(Map<D, ?> map) {
        return map.keySet().iterator().next();
    }

    private static <D> D otherKey(Map<D, ?> map, D exclude) {
        for (D key : map.keySet()) {
            if (!key.equals(exclude))
                return key;
        }
        throw new IllegalStateException("group needs at least 2 buttons for this test");
    }

    /**
     * Recipe: same trimmed-{@code group-layers} approach as {@code
     * ClickThroughInputActionTest#buildThematicController}, but {@code initialVisibility = false}
     * (matching the real shipped {@code assembly.yaml}) instead of {@code true} — see class javadoc.
     */
    private static ThematicSetup buildThematicController(GroupKey key) {
        DeckMapConfig deckMapConfig = DeckMapConfig.createDefault();
        TopologyModel topology = deckMapConfig.getTopologyConfig().buildTopologyModel();

        DeckMapAssemblyConfig realAssembly = DeckMapAssemblyConfig.createDefault(
            deckMapConfig.getGroupsConfig(), topology.allHydrantOutlets());
        DeckMapAssemblyConfig thematicAssembly = new DeckMapAssemblyConfig(
            List.of(new GroupLayerSpec(key, false)), realAssembly.hydrantButtonGroupSpecList());

        ImageIcon mapImage = new ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()));
        DeckMapController controller = new DeckMapController(thematicAssembly, deckMapConfig, topology, mapImage, event -> {});
        return new ThematicSetup(controller);
    }

    private record ThematicSetup(DeckMapController controller) {}
}
