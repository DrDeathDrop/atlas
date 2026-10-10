package io.github.drdeathdrop.atlas.facility.registry;

import java.util.UUID;

public class FacilityNotFoundException extends RuntimeException {
    public FacilityNotFoundException(UUID id) {
        super("Facility " + id + " was not found");
    }
}
