package io.github.drdeathdrop.atlas.facility.registry;

public class OccupancyOverCapacityException extends RuntimeException {
    public OccupancyOverCapacityException(int occupancy, int capacity) {
        super("Occupancy " + occupancy + " is more than the capacity of " + capacity);
    }
}
