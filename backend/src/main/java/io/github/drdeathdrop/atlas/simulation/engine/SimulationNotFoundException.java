package io.github.drdeathdrop.atlas.simulation.engine;

import java.util.UUID;

public class SimulationNotFoundException extends RuntimeException {
    public SimulationNotFoundException(UUID id) {
        super("Simulation " + id + " was not found");
    }
}
