package io.github.drdeathdrop.atlas.facility.registry;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FacilityExceptionHandler {
    @ExceptionHandler(FacilityNotFoundException.class)
    public ProblemDetail handleNotFound(FacilityNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(FacilityNameAlreadyUsedException.class)
    public ProblemDetail handleNameAlreadyUsed(FacilityNameAlreadyUsedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OccupancyOverCapacityException.class)
    public ProblemDetail handleOverCapacity(OccupancyOverCapacityException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
