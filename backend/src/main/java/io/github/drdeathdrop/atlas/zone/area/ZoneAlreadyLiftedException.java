package io.github.drdeathdrop.atlas.zone.area;

import java.util.UUID;

public class ZoneAlreadyLiftedException extends RuntimeException {
    public ZoneAlreadyLiftedException(UUID id) {
        super("Zone " + id + " has already been lifted");
    }
}
