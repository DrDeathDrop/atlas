package io.github.drdeathdrop.atlas.simulation.engine;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SimulationExceptionHandler {
    @ExceptionHandler(SimulationNotFoundException.class)
    public ProblemDetail handleNotFound(SimulationNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({SimulationAlreadyRunningException.class, SimulationNotResumableException.class})
    public ProblemDetail handleAlreadyRunning(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
