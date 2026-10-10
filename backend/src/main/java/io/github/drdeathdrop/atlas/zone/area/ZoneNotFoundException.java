package io.github.drdeathdrop.atlas.zone.area;

import java.util.UUID;

public class ZoneNotFoundException extends RuntimeException {
    public ZoneNotFoundException(UUID id) {
        super("Zone " + id + " was not found");
    }
}
