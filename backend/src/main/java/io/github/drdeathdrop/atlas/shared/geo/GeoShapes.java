package io.github.drdeathdrop.atlas.shared.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GeoShapes {
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), GeoPoints.WGS84);

    private GeoShapes() {
    }

    public static Polygon polygon(List<GeoPosition> corners) {
        if (corners == null || corners.size() < 3) {
            throw new InvalidShapeException("An area needs at least three corners");
        }

        List<Coordinate> ring = new ArrayList<>(coordinates(corners));
        Coordinate first = ring.get(0);
        if (!first.equals2D(ring.get(ring.size() - 1))) {
            ring.add(new Coordinate(first));
        }
        if (ring.size() < 4) {
            throw new InvalidShapeException("An area needs at least three different corners");
        }

        Polygon polygon = FACTORY.createPolygon(ring.toArray(Coordinate[]::new));
        if (polygon.isEmpty() || !polygon.isValid()) {
            throw new InvalidShapeException("The edges of an area must not cross each other");
        }
        return polygon;
    }

    public static LineString line(List<GeoPosition> points) {
        if (points == null || points.size() < 2) {
            throw new InvalidShapeException("A line needs at least two points");
        }

        LineString line = FACTORY.createLineString(coordinates(points).toArray(Coordinate[]::new));
        if (line.getLength() == 0) {
            throw new InvalidShapeException("A line needs at least two different points");
        }
        return line;
    }

    public static List<GeoPosition> positions(Polygon polygon) {
        Coordinate[] ring = polygon.getExteriorRing().getCoordinates();
        return positions(Arrays.copyOf(ring, ring.length - 1));
    }

    public static List<GeoPosition> positions(LineString line) {
        return positions(line.getCoordinates());
    }

    private static List<Coordinate> coordinates(List<GeoPosition> positions) {
        return positions.stream()
                .map(position -> GeoPoints.of(position.latitude(), position.longitude()).getCoordinate())
                .toList();
    }

    private static List<GeoPosition> positions(Coordinate[] coordinates) {
        return Arrays.stream(coordinates)
                .map(coordinate -> new GeoPosition(coordinate.getY(), coordinate.getX()))
                .toList();
    }
}
