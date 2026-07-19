package gui.map.view.painting;

import config.loading.DeckMapGeometryConfig;
import config.specification.basic.BorderCoordinateSpec;
import config.specification.ElementPlacement;
import config.specification.basic.LocationCoordinateSpec;
import domain.Link;
import domain.Location;
import geometry.Point;
import geometry.Polygon;
import geometry.Polyline;

import java.util.*;

public class DeckMapGeometry {
    private final Map<String, Polygon> polygonByCode;
    private final Map<String, Polyline> polylineByLink;
    private final Map<String, Point> explosionPreventionMarkers;

    public DeckMapGeometry(DeckMapGeometryConfig geometryConfig, List<ElementPlacement<String>> explosionPreventionMarkers) {
        this.explosionPreventionMarkers = buildExplosionMarkersIndex(explosionPreventionMarkers);
        this.polygonByCode = buildPolygonByCode(geometryConfig);
        this.polylineByLink = buildPolylineByLink(geometryConfig);
    }

    public List<Polygon> toLocationPolygons(Set<Location> locations) {
        List<Polygon> polygons = new ArrayList<>();
        for (Location location : locations) {
            Polygon polygon = polygonByCode.get(location.getCode().toUpperCase());
            if (polygon != null)
                polygons.add(polygon);
        }
        return polygons;
    }

    public List<Polyline> toFireLinePolylines(List<Link> fireLineLinks) {
        List<Polyline> polylines = new ArrayList<>();
        for (Link link : fireLineLinks) {
            Polyline polyline = polylineByLink.get(link.getCode().toUpperCase());
            if (polyline != null)
                polylines.add(polyline);
        }
        return polylines;
    }

    public List<Point> toExplosionMarkers(Set<Location> sourceLocations) {
        return sourceLocations.stream()
            .map(location -> explosionPreventionMarkers.get(location.getCode().toUpperCase()))
            .filter(Objects::nonNull)
            .toList();
    }

    private static Map<String, Point> buildExplosionMarkersIndex(List<ElementPlacement<String>> explosionPreventionMarkers) {
        Map<String, Point> map = new LinkedHashMap<>();
        if (explosionPreventionMarkers != null) {
            for (ElementPlacement<String> ep : explosionPreventionMarkers)
                map.put(ep.code().toUpperCase(), ep.point());
        }
        return map;
    }

    private static Map<String, Polygon> buildPolygonByCode(DeckMapGeometryConfig geometryConfig) {
        Map<String, Polygon> map = new LinkedHashMap<>();
        List<LocationCoordinateSpec> coords = geometryConfig != null ? geometryConfig.locationCoordinates() : null;
        if (coords != null) {
            for (LocationCoordinateSpec spec : coords) {
                if (spec != null && spec.code() != null && spec.points() != null)
                    map.put(spec.code().toUpperCase(), new Polygon(spec.points()));
            }
        }
        return map;
    }

    private static Map<String, Polyline> buildPolylineByLink(DeckMapGeometryConfig geometryConfig) {
        Map<String, Polyline> map = new LinkedHashMap<>();
        List<BorderCoordinateSpec> coords = geometryConfig != null ? geometryConfig.borderCoordinates() : null;
        if (coords != null) {
            for (BorderCoordinateSpec spec : coords) {
                if (spec != null && spec.link() != null && spec.points() != null)
                    map.put(spec.link().toUpperCase(), new Polyline(spec.points()));
            }
        }
        return map;
    }
}
