package io.github.drdeathdrop.atlas.facility.registry;

public class FacilityNameAlreadyUsedException extends RuntimeException {
    public FacilityNameAlreadyUsedException(String name) {
        super("A facility named " + name + " already exists");
    }
}
