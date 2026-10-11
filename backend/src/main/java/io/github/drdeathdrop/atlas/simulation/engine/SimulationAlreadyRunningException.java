package io.github.drdeathdrop.atlas.simulation.engine;

public class SimulationAlreadyRunningException extends RuntimeException {
    public SimulationAlreadyRunningException() {
        super("A simulation is already running. Stop it or wait for it to finish");
    }
}
