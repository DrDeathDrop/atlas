package io.github.drdeathdrop.atlas.zone.closure;

import java.util.UUID;

public class RoadAlreadyReopenedException extends RuntimeException {
    public RoadAlreadyReopenedException(UUID id) {
        super("Road closure " + id + " has already been reopened");
    }
}
