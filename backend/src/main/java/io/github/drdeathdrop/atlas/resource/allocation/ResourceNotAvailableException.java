package io.github.drdeathdrop.atlas.resource.allocation;

import io.github.drdeathdrop.atlas.resource.ResourceStatus;

public class ResourceNotAvailableException extends RuntimeException {
    public ResourceNotAvailableException(String callSign, ResourceStatus status) {
        super(callSign + " cannot be assigned because it is " + status);
    }
}
