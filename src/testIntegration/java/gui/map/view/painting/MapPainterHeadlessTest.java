package gui.map.view.painting;

import config.loading.DeckMapControlsConfig;
import config.loading.DeckMapGeometryConfig;
import config.loading.DeckMapTopologyConfig;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import gui.Localization;
import gui.map.state.FireIncidentState;
import gui.map.state.HydrantViewData;
import gui.map.state.InputControlsData;
import gui.map.state.PaintingViewData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.ResourceUtil;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Regression guard (originally a narrow spike, see {@code src/testIntegration/java/AGENTS.md}, that
 * confirmed the real map-image loading + painting pipeline every {@code DeckMapController}
 * unconditionally builds first — {@code new
 * ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()))} followed by {@link
 * MapPainter}'s construction — works in a headless JVM, {@code -Djava.awt.headless=true}, forced on
 * the {@code testIntegration} Test task; now permanent coverage for that same boundary). This is the
 * one risk that a "thematic" (selectively-configured) {@code DeckMapController} cannot route around:
 * every controller builds this pipeline regardless of which button groups it configures. Deliberately
 * does not construct a full {@code DeckMapController} — only the map-image/painting slice it owns.
 */
class MapPainterHeadlessTest {
    private static final int FIRE_LINE_WIDTH = 12;

    private static TopologyModel topology;
    private static DeckMapGeometry geometry;
    private static ImageIcon mapImage;

    @BeforeAll
    static void loadSharedFixtures() {
        topology = DeckMapTopologyConfig.createDefault().buildTopologyModel();
        geometry = new DeckMapGeometry(
            DeckMapGeometryConfig.createDefault(), DeckMapControlsConfig.createDefault().explosionPreventionMarkers());
        mapImage = new ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()));
    }

    @Test
    void constructs_WithRealImage() {
        ImageIcon mapImage = new ImageIcon(ResourceUtil.resolveResourceUrl(Localization.getMapImageFile()));

        // Real decode succeeded (a broken/undecoded ImageIcon reports -1 for both dimensions).
        assertThat(mapImage.getIconWidth(), greaterThan(0));
        assertThat(mapImage.getIconHeight(), greaterThan(0));

        DeckMapGeometry geometry = new DeckMapGeometry(
            DeckMapGeometryConfig.createDefault(), DeckMapControlsConfig.createDefault().explosionPreventionMarkers());
        FireIncidentState fireIncidentState = new FireIncidentState();

        MapPainter mapPainter = assertDoesNotThrow(
            () -> new MapPainter(mapImage, geometry, fireIncidentState, FIRE_LINE_WIDTH));

        JPanel rootContainer = mapPainter.getRootContainer();
        JLabel upperLayer = mapPainter.getUpperLayer();
        assertThat(rootContainer, notNullValue());
        assertThat(upperLayer, notNullValue());
    }

    /**
     * {@code MapPainter}'s three overlay panels (fire lines, explosion markers, filled locations)
     * override {@code paintComponent} with real {@code Graphics} drawing calls (fillOval,
     * fillPolygon, fillRoundRect) — but the classes are private, so the only accessible way to
     * exercise that real code path from outside is {@code Component#paint}, which is public and
     * cascades into {@code paintComponent}/{@code paintChildren} for the whole nested panel
     * hierarchy. Renders onto an offscreen {@link BufferedImage} (no native peer needed), same
     * technique as {@code HeadlessSwingButtonTest}.
     */
    @Test
    void paint_WholeOverlayHierarchy() {
        FireIncidentState fireIncidentState = new FireIncidentState();
        MapPainter mapPainter = new MapPainter(mapImage, geometry, fireIncidentState, FIRE_LINE_WIDTH);

        JPanel rootContainer = mapPainter.getRootContainer();
        rootContainer.setBounds(0, 0, mapImage.getIconWidth(), mapImage.getIconHeight());
        BufferedImage offscreen = new BufferedImage(mapImage.getIconWidth(), mapImage.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = offscreen.createGraphics();

        assertDoesNotThrow(() -> rootContainer.paint(graphics));
        graphics.dispose();
    }

    /**
     * Same offscreen paint path as {@link #paint_WholeOverlayHierarchy}, but with a non-empty
     * {@link FireIncidentState}: fire/threat/evacuation polygons, a real fire-line border, and a
     * pending explosion-prevention marker. This exercises the contentful branches of the private
     * {@code PaintFilledLocations}/{@code PaintFireLines}/{@code PaintExplosions} overlays that the
     * empty-state paint test never reaches.
     */
    @Test
    void paint_WithNonEmptyIncidentState_DrawsOverlaysWithoutException() {
        Location fireRoom = topology.location("a");
        Location threatened = topology.location("b");
        Location evacuation = topology.location("e");
        Location explosionThreat = topology.location("j");
        Link fireLine = topology.link("ae");

        FireIncidentState fireIncidentState = new FireIncidentState();
        fireIncidentState.updateState(
            new PaintingViewData(Set.of(fireRoom), Set.of(threatened, evacuation), List.of(fireLine)),
            HydrantViewData.EMPTY,
            new InputControlsData(Set.of(evacuation), Set.of(), List.of(), List.of(), Set.of(), Set.of()),
            Set.of(explosionThreat));

        MapPainter mapPainter = new MapPainter(mapImage, geometry, fireIncidentState, FIRE_LINE_WIDTH);
        fireIncidentState.addMapDrawingListener(mapPainter);

        JPanel rootContainer = mapPainter.getRootContainer();
        rootContainer.setBounds(0, 0, mapImage.getIconWidth(), mapImage.getIconHeight());
        BufferedImage offscreen = new BufferedImage(mapImage.getIconWidth(), mapImage.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = offscreen.createGraphics();

        assertDoesNotThrow(() -> rootContainer.paint(graphics));
        graphics.dispose();

        assertThat(geometry.toLocationPolygons(Set.of(fireRoom)), hasSize(greaterThan(0)));
        assertThat(geometry.toFireLinePolylines(List.of(fireLine)), hasSize(greaterThan(0)));
        assertThat(geometry.toExplosionMarkers(Set.of(explosionThreat)), hasSize(greaterThan(0)));
    }
}
