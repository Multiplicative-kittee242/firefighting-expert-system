package gui.map.view.painting;

import gui.map.ColorPalette;
import domain.Location;
import gui.map.state.FireIncidentState;
import gui.map.state.MapDrawingListener;
import gui.map.state.PaintingViewData;
import geometry.Point;
import geometry.Polygon;
import geometry.Polyline;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the map overlay rendering: the panel hierarchy and the Paint* components that draw
 * fire/threat/evacuation zones, explosion markers and fire-line boundaries on top of the deck map image.
 */
public class MapPainter implements MapDrawingListener {
    private final DeckMapGeometry geometry;
    private final FireIncidentState fireIncidentState;

    private final JPanel filledLocationsPanel;
    private final JLabel mapImageLabel;

    private final List<JPanel> overlays = new ArrayList<>();

    public MapPainter(ImageIcon mapImage, DeckMapGeometry geometry, FireIncidentState fireIncidentState, int fireLineWidth) {
        this.geometry = geometry;
        this.fireIncidentState = fireIncidentState;

        PaintFilledLocations filledLocationsOverlay = new PaintFilledLocations();
        PaintExplosions explosionMarkersOverlay = new PaintExplosions();
        PaintFireLines fireLinesOverlay = new PaintFireLines(fireLineWidth);

        overlays.add(filledLocationsOverlay);
        overlays.add(explosionMarkersOverlay);
        overlays.add(fireLinesOverlay);

        filledLocationsPanel = filledLocationsOverlay;
        filledLocationsPanel.setOpaque(false);
        filledLocationsPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        filledLocationsPanel.add(explosionMarkersOverlay);

        explosionMarkersOverlay.setOpaque(false);
        explosionMarkersOverlay.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        explosionMarkersOverlay.add(fireLinesOverlay);

        fireLinesOverlay.setOpaque(false);
        fireLinesOverlay.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        mapImageLabel = new JLabel(mapImage);
        mapImageLabel.setOpaque(false);
        mapImageLabel.setBackground(Color.white);
        mapImageLabel.setVerticalTextPosition(1);
        fireLinesOverlay.add(mapImageLabel);
    }

    public JPanel getRootContainer() {
        return filledLocationsPanel;
    }

    public JLabel getUpperLayer() {
        return mapImageLabel;
    }

    @Override
    public void onMapDrawingDataChanged(PaintingViewData data) {
        repaint();
    }

    public void repaint() {
        for (JPanel overlay : overlays)
            overlay.repaint();
    }

    //================================================================
    // MAP OVERLAYS (Paint* classes)
    // Inner classes for drawing map overlays (evacuation, explosions, fire lines).
    //================================================================

    private class PaintFireLines extends JPanel {
        private final int lineWidth;

        public PaintFireLines(int lineWidth) {
            this.lineWidth = lineWidth;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(ColorPalette.MID_DARK_RED);
            for (Polyline polyline : geometry.toFireLinePolylines(fireIncidentState.getFireLines())) {
                List<Point> points = polyline.points();
                for (int i = 0; i < points.size() - 1; ++i) {
                    Point start = points.get(i);
                    Point end = points.get(i + 1);
                    fillBoldLine(g, start, end, lineWidth);
                }
            }
        }

        private static void fillBoldLine(Graphics g, Point start, Point end, int lineWidth) {
            g.fillRoundRect(
                    Math.min(start.x(), end.x()) - lineWidth / 2,
                    Math.min(start.y(), end.y()) - lineWidth / 2,
                    Math.abs(end.x() - start.x()) + lineWidth,
                    Math.abs(end.y() - start.y()) + lineWidth,
                    lineWidth / 2,
                    lineWidth / 2
            );
        }
    }

    private class PaintExplosions extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Set<Location> pendingLocations = fireIncidentState.fetchPendingExplosionPreventionLocations();
            List<Point> explosionMarkers = geometry.toExplosionMarkers(pendingLocations);
            for (Point dot : explosionMarkers)
                drawExplosionMarker(g, dot);
        }

        private void drawExplosionMarker(Graphics g, Point dot) {
            g.setColor(ColorPalette.DARK_RED);
            g.fillOval(dot.x(), dot.y(), 29, 29);
            g.setColor(ColorPalette.RED);
            g.fillOval(dot.x() + 1, dot.y() + 1, 27, 27);
        }
    }

    private class PaintFilledLocations extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            fillPolygons(g, ColorPalette.PASTELLE_RED, geometry.toLocationPolygons(fireIncidentState.getFireLocations()));
            fillPolygons(g, ColorPalette.PASTELLE_ORANGE, geometry.toLocationPolygons(fireIncidentState.getThreatenedLocations()));
            fillPolygons(g, ColorPalette.PASTELLE_GREY, geometry.toLocationPolygons(fireIncidentState.getEvacuationLocations()));
        }

        private void fillPolygons(Graphics g, Color color, List<Polygon> polygons) {
            g.setColor(color);
            for (Polygon polygon : polygons)
                fillPolygon(g, polygon);
        }

        private void fillPolygon(Graphics g, Polygon polygon) {
            List<Point> points = polygon.points();
            int pointCount = points.size();
            int[] xs = new int[pointCount];
            int[] ys = new int[pointCount];
            for (int i = 0; i < pointCount; i++) {
                xs[i] = points.get(i).x();
                ys[i] = points.get(i).y();
            }
            g.fillPolygon(xs, ys, pointCount);
        }
    }
}
