package io.github.drdeathdrop.atlas.zone.closure;

import java.util.UUID;

public class RoadClosureNotFoundException extends RuntimeException {
    public RoadClosureNotFoundException(UUID id) {
        super("Road closure " + id + " was not found");
    }
}
