package gui.map.input;

import clips.values.EvacuationStatus;
import config.loading.DeckMapTopologyConfig;
import config.specification.ElementPlacement;
import domain.Location;
import domain.registry.TopologyModel;
import domain.types.FireSensorType;
import geometry.Point;
import gui.map.input.controls.EvacuationButton;
import gui.map.input.controls.FireSensorButton;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Regression guard (originally a narrow spike, see {@code src/testIntegration/java/AGENTS.md}, that
 * confirmed {@code gui.map.input} button groups/controls can be constructed and exercised in a
 * headless JVM — {@code -Djava.awt.headless=true}, set on the {@code testIntegration} Test task in
 * {@code build.gradle} — before any GUI-facing integration test relied on it; now permanent
 * coverage for that same boundary). Deliberately does not construct a full {@code
 * DeckMapController} — only the isolated pieces this package owns, targeting the boundary calls
 * most likely to fail headless: a real {@code JToggleButton}'s {@code doClick()}, and {@code
 * Graphics}/{@code FontMetrics} usage outside the normal Swing paint pipeline.
 */
class HeadlessSwingButtonTest {

    @Test
    void collectChanges_ReturnsUnselectedStatusHeadless() {
        TopologyModel topology = DeckMapTopologyConfig.createDefault().buildTopologyModel();
        Location location = topology.location("a");
        List<ElementPlacement<String>> elements = List.of(ElementPlacement.raw("a", new Point(10, 10)));
        EvacuationButtonGroup group = new EvacuationButtonGroup(elements, topology);

        Map<Location, EvacuationStatus> changes = group.collectChanges(location);

        assertThat(changes, is(Map.of(location, EvacuationStatus.NONE)));
    }

    @Test
    void doClick_TogglesSelectedStateHeadless() {
        EvacuationButton button = new EvacuationButton();

        button.doClick();

        assertThat(button.isSelected(), is(true));
    }

    /**
     * {@link FireSensorButton#drawContent} calls {@code drawCenteredString}, which reads
     * {@code Graphics#getFontMetrics()} directly (not through the normal repaint pipeline) and
     * resolves its label via {@code gui.Localization} — the two boundary calls most likely to
     * assume a display exists. Renders onto an offscreen {@link BufferedImage}, which needs no
     * native peer, so this exercises those calls without a realized window.
     */
    @Test
    void paintComponent_PaintsOffscreenWithoutExceptionHeadless() {
        FireSensorButton button = new FireSensorButton(FireSensorType.COMBINED, 19);
        BufferedImage offscreen = new BufferedImage(19, 19, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = offscreen.createGraphics();

        assertDoesNotThrow(() -> button.paintComponent(graphics));
    }
}
