package io.github.drdeathdrop.atlas.simulation.engine;

public class SimulationNotResumableException extends RuntimeException {
    public SimulationNotResumableException() {
        super("Only a stopped simulation can be resumed");
    }
}
