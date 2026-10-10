package io.github.drdeathdrop.atlas.resource.inventory;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(UUID id) {
        super("Resource " + id + " was not found");
    }
}
