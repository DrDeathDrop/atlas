package io.github.drdeathdrop.atlas.shared.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeoPoints {
    public static final int WGS84 = 4326;

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), WGS84);

    private GeoPoints() {
    }

    public static Point of(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        return FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    public static double latitude(Point point) {
        return point.getY();
    }

    public static double longitude(Point point) {
        return point.getX();
    }
}
