package io.github.drdeathdrop.atlas.resource.inventory;

public class CallSignAlreadyUsedException extends RuntimeException {
    public CallSignAlreadyUsedException(String callSign) {
        super("A resource with call sign " + callSign + " already exists");
    }
}
