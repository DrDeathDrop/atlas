package io.github.drdeathdrop.atlas.resource.allocation;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AllocationExceptionHandler {
    @ExceptionHandler(ResourceNotAvailableException.class)
    public ProblemDetail handleNotAvailable(ResourceNotAvailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(AssignmentAlreadyReleasedException.class)
    public ProblemDetail handleAlreadyReleased(AssignmentAlreadyReleasedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(AssignmentNotFoundException.class)
    public ProblemDetail handleAssignmentNotFound(AssignmentNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail handleBusy(PessimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The resource is being changed by another request. Try again.");
    }
}
