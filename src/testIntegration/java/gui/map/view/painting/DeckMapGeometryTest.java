package gui.map.view.painting;

import config.loading.DeckMapControlsConfig;
import config.loading.DeckMapGeometryConfig;
import config.loading.DeckMapTopologyConfig;
import domain.Link;
import domain.Location;
import domain.registry.TopologyModel;
import geometry.Point;
import geometry.Polygon;
import geometry.Polyline;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Covers the lookup branches of {@link DeckMapGeometry} against real shipped geometry.yaml /
 * controls.yaml data: known codes resolve to geometry, unknown codes are filtered out (not thrown).
 */
class DeckMapGeometryTest {
    private static TopologyModel topology;
    private static DeckMapGeometry geometry;

    @BeforeAll
    static void loadSharedFixtures() {
        topology = DeckMapTopologyConfig.createDefault().buildTopologyModel();
        geometry = new DeckMapGeometry(
            DeckMapGeometryConfig.createDefault(), DeckMapControlsConfig.createDefault().explosionPreventionMarkers());
    }

    @Test
    void toLocationPolygons_ResolvesKnownCodes_SkipsUnknown() {
        Location known = topology.location("a");
        Location unknown = new Location("zz");

        List<Polygon> polygons = geometry.toLocationPolygons(Set.of(known, unknown));

        assertThat(polygons, hasSize(1));
    }

    @Test
    void toLocationPolygons_EmptyInput_ReturnsEmptyList() {
        assertThat(geometry.toLocationPolygons(Set.of()), empty());
    }

    @Test
    void toFireLinePolylines_ResolvesKnownLinks_SkipsUnknown() {
        Link known = topology.link("ae");
        Link unknown = new Link("ZZ", new Location("z"), new Location("y"));

        List<Polyline> polylines = geometry.toFireLinePolylines(List.of(known, unknown));

        assertThat(polylines, hasSize(1));
    }

    @Test
    void toFireLinePolylines_EmptyInput_ReturnsEmptyList() {
        assertThat(geometry.toFireLinePolylines(List.of()), empty());
    }

    @Test
    void toExplosionMarkers_ResolvesKnownCodes_SkipsUnknown() {
        Location known = topology.location("j");
        Location unknown = new Location("zz");

        List<Point> markers = geometry.toExplosionMarkers(Set.of(known, unknown));

        assertThat(markers, hasSize(1));
    }

    @Test
    void toExplosionMarkers_EmptyInput_ReturnsEmptyList() {
        assertThat(geometry.toExplosionMarkers(Set.of()), empty());
    }

    @Test
    void lookups_AreCaseInsensitiveOnCodes() {
        String locationCode = "a";
        Location lower = topology.location(locationCode);
        Location upperIdentity = new Location(locationCode.toUpperCase());
        String linkCode = "ae";
        Link lowerLink = topology.link(linkCode);
        Link upperLink = new Link(linkCode.toUpperCase(), topology.location("a"), topology.location("e"));

        assertThat(geometry.toLocationPolygons(Set.of(upperIdentity)), hasSize(1));
        assertThat(geometry.toLocationPolygons(Set.of(lower)), hasSize(1));
        assertThat(geometry.toFireLinePolylines(List.of(upperLink)), hasSize(1));
        assertThat(geometry.toFireLinePolylines(List.of(lowerLink)), hasSize(1));
    }
}
