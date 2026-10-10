package io.github.drdeathdrop.atlas.resource.allocation;

import java.util.UUID;

public class AssignmentNotFoundException extends RuntimeException {
    public AssignmentNotFoundException(UUID id) {
        super("Assignment " + id + " was not found");
    }
}
