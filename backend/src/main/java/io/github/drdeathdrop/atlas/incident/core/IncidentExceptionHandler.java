package io.github.drdeathdrop.atlas.incident.core;

import io.github.drdeathdrop.atlas.incident.dispatch.IncidentNotDispatchableException;
import io.github.drdeathdrop.atlas.incident.lifecycle.InvalidTransitionException;
import io.github.drdeathdrop.atlas.incident.lifecycle.TransitionNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IncidentExceptionHandler {
    @ExceptionHandler(IncidentNotFoundException.class)
    public ProblemDetail handleNotFound(IncidentNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidTransitionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IncidentNotDispatchableException.class)
    public ProblemDetail handleNotDispatchable(IncidentNotDispatchableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(TransitionNotPermittedException.class)
    public ProblemDetail handleNotPermitted(TransitionNotPermittedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }
}
