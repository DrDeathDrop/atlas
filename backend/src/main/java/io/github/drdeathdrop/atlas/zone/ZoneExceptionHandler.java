package io.github.drdeathdrop.atlas.zone;

import io.github.drdeathdrop.atlas.shared.geo.InvalidShapeException;
import io.github.drdeathdrop.atlas.zone.area.ZoneAlreadyLiftedException;
import io.github.drdeathdrop.atlas.zone.area.ZoneNotFoundException;
import io.github.drdeathdrop.atlas.zone.closure.RoadAlreadyReopenedException;
import io.github.drdeathdrop.atlas.zone.closure.RoadClosureNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ZoneExceptionHandler {
    @ExceptionHandler({ZoneNotFoundException.class, RoadClosureNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({ZoneAlreadyLiftedException.class, RoadAlreadyReopenedException.class})
    public ProblemDetail handleAlreadyClosed(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidShapeException.class)
    public ProblemDetail handleInvalidShape(InvalidShapeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
