package gui.map;

import config.groups.GroupKey;
import config.loading.DeckMapAssemblyConfig;
import config.loading.DeckMapConfig;
import config.specification.GroupLayerSpec;
import domain.Extinguisher;
import domain.Location;
import domain.registry.TopologyModel;
import gui.Localization;
import gui.map.values.ExtinguisherUsage;
import org.junit.jupiter.api.Test;
import util.ResourceUtil;

import javax.swing.ImageIcon;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@link DeckMapController} only constructs the button groups its assembly config
 * declares (see the constructor's {@code activeGroups} check) — the refactor that lets a test
 * build a "thematic" controller containing just the group(s) it needs, real end to end, instead
 * of always paying for all 8 input-control groups. Also documents the recipe for building such a
 * trimmed {@link DeckMapAssemblyConfig}: keep the real hydrant-button-group specs (those groups
 * are still constructed unconditionally) but trim {@code group-layers} down to the groups wanted.
 * <p>
 * Lives in package {@code gui.map} (same as {@link DeckMapController}) so it can call its
 * package-private {@code @VisibleForTesting} group getters.
 */
class DeckMapControllerSelectiveGroupConstructionTest {

    @Test
    void constructs_OnlyGroupsInAssemblyConfig() {
        DeckMapConfig deckMapConfig = DeckMapConfig.createDefault();
        TopologyModel topology = deckMapConfig.getTopologyConfig().buildTopologyModel();

        DeckMapAssemblyConfig realAssembly = DeckMapAssemblyConfig.createDefault(
            deckMapConfig.getGroupsConfig(), topology.allHydrantOutlets());
        // Thematic recipe: keep the real hydrant-button-group specs (those groups are still
        // constructed unconditionally, out of scope for this refactor) but trim group-layers
        // down to only the group this test cares about.
        DeckMapAssemblyConfig thematicAssembly = new DeckMapAssemblyConfig(
            List.of(new GroupLayerSpec(GroupKey.EVACUATION_GROUP, true)),
            realAssembly.hydrantButtonGroupSpecList());

        ImageIcon mapImage = new ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()));
        DeckMapController controller = new DeckMapController(
            thematicAssembly, deckMapConfig, topology, mapImage, event -> {});

        // Only the declared group was constructed.
        assertThat(controller.inputGroups().getEvacuationGroup(), notNullValue());

        // Every other input-control group stayed null.
        assertThat(controller.inputGroups().getVentilationGroup(), nullValue());
        assertThat(controller.inputGroups().getDoorSealingGroup(), nullValue());
        assertThat(controller.inputGroups().getExplosionGroup(), nullValue());
        assertThat(controller.inputGroups().getFlammableGroup(), nullValue());
        assertThat(controller.inputGroups().getMachineryDamageGroup(), nullValue());
        assertThat(controller.inputGroups().getExtinguisherGroup(), nullValue());

        // The configured group's collect*Changes works normally.
        Location location = topology.location("a");
        assertThat(controller.collectEvacChanges(location), notNullValue());

        // An unconfigured group fails fast with a clear message...
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> controller.collectVentChanges(location));
        assertThat(exception.getMessage(), containsString("VENTILATION_GROUP"));

        // ...except extinguishers, whose absence is a legitimate empty state, not an error.
        Map<Extinguisher, ExtinguisherUsage> extinguisherChanges =
            controller.collectExtinguisherChanges(topology.allExtinguishers().get(0));
        assertThat(extinguisherChanges, is(anEmptyMap()));
    }
}
