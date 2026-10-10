package io.github.drdeathdrop.atlas.resource.inventory;

public class ResourceOnAssignmentException extends RuntimeException {
    public ResourceOnAssignmentException(String callSign) {
        super(callSign + " is assigned to an incident. Release it before changing whether it is in service");
    }
}
