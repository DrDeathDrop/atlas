package io.github.drdeathdrop.atlas.shared.geo;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoShapesTest {

    private static final GeoPosition SOUTH_WEST = new GeoPosition(42.10, 24.70);
    private static final GeoPosition SOUTH_EAST = new GeoPosition(42.10, 24.80);
    private static final GeoPosition NORTH_EAST = new GeoPosition(42.20, 24.80);
    private static final GeoPosition NORTH_WEST = new GeoPosition(42.20, 24.70);

    @Test
    void polygonClosesTheRingAndUsesWgs84() {
        Polygon polygon = GeoShapes.polygon(List.of(SOUTH_WEST, SOUTH_EAST, NORTH_EAST, NORTH_WEST));

        assertThat(polygon.getSRID()).isEqualTo(GeoPoints.WGS84);
        assertThat(polygon.getExteriorRing().getNumPoints()).isEqualTo(5);
        assertThat(polygon.getExteriorRing().isClosed()).isTrue();
        assertThat(polygon.getExteriorRing().getCoordinateN(0).getX()).isEqualTo(24.70);
        assertThat(polygon.getExteriorRing().getCoordinateN(0).getY()).isEqualTo(42.10);
    }

    @Test
    void polygonAcceptsARingThatIsAlreadyClosed() {
        Polygon polygon = GeoShapes.polygon(List.of(SOUTH_WEST, SOUTH_EAST, NORTH_EAST, SOUTH_WEST));

        assertThat(polygon.getExteriorRing().getNumPoints()).isEqualTo(4);
    }

    @Test
    void positionsOfAPolygonAreItsCornersWithoutTheClosingPoint() {
        List<GeoPosition> corners = List.of(SOUTH_WEST, SOUTH_EAST, NORTH_EAST, NORTH_WEST);

        assertThat(GeoShapes.positions(GeoShapes.polygon(corners))).containsExactlyElementsOf(corners);
    }

    @Test
    void polygonNeedsAtLeastThreeCorners() {
        assertThatThrownBy(() -> GeoShapes.polygon(List.of(SOUTH_WEST, SOUTH_EAST)))
                .isInstanceOf(InvalidShapeException.class);
    }

    @Test
    void polygonNeedsThreeDifferentCorners() {
        assertThatThrownBy(() -> GeoShapes.polygon(List.of(SOUTH_WEST, SOUTH_EAST, SOUTH_WEST)))
                .isInstanceOf(InvalidShapeException.class);
    }

    @Test
    void polygonRejectsEdgesThatCross() {
        assertThatThrownBy(() -> GeoShapes.polygon(List.of(SOUTH_WEST, NORTH_EAST, SOUTH_EAST, NORTH_WEST)))
                .isInstanceOf(InvalidShapeException.class);
    }

    @Test
    void lineKeepsThePointsInOrder() {
        List<GeoPosition> points = List.of(SOUTH_WEST, NORTH_EAST, NORTH_WEST);

        LineString line = GeoShapes.line(points);

        assertThat(line.getSRID()).isEqualTo(GeoPoints.WGS84);
        assertThat(GeoShapes.positions(line)).containsExactlyElementsOf(points);
    }

    @Test
    void lineNeedsAtLeastTwoPoints() {
        assertThatThrownBy(() -> GeoShapes.line(List.of(SOUTH_WEST)))
                .isInstanceOf(InvalidShapeException.class);
    }

    @Test
    void lineNeedsTwoDifferentPoints() {
        assertThatThrownBy(() -> GeoShapes.line(List.of(SOUTH_WEST, SOUTH_WEST)))
                .isInstanceOf(InvalidShapeException.class);
    }
}
