package io.github.drdeathdrop.atlas.resource.allocation;

import java.util.UUID;

public class AssignmentAlreadyReleasedException extends RuntimeException {
    public AssignmentAlreadyReleasedException(UUID id) {
        super("Assignment " + id + " has already been released");
    }
}
