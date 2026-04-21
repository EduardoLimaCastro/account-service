package com.eduardo.account_service.infrastructure.web.exception;

import com.eduardo.account_service.domain.exceptions.AccountNotFoundException;
import com.eduardo.account_service.domain.exceptions.AgencyNotFoundException;
import com.eduardo.account_service.domain.exceptions.AgencyNotActiveException;
import com.eduardo.account_service.domain.exceptions.InvalidAccountStateTransitionException;
import com.eduardo.account_service.domain.exceptions.OwnerNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> "'%s' %s".formatted(fe.getField(), fe.getDefaultMessage()))
                .toList();

        log.warn("Validation failed at {}: {}", request.getRequestURI(), errors);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more fields failed validation");
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("/errors/validation-failed"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            AccountNotFoundException ex, HttpServletRequest request) {

        log.warn("Account not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Account Not Found");
        problem.setType(URI.create("/errors/not-found"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(OwnerNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOwnerNotFound(
            OwnerNotFoundException ex, HttpServletRequest request) {

        log.warn("Owner not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Owner Not Found");
        problem.setType(URI.create("/errors/owner-not-found"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(AgencyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAgencyNotFound(
            AgencyNotFoundException ex, HttpServletRequest request) {

        log.warn("Agency not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Agency Not Found");
        problem.setType(URI.create("/errors/agency-not-found"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(AgencyNotActiveException.class)
    public ResponseEntity<ProblemDetail> handleAgencyNotActive(
            AgencyNotActiveException ex, HttpServletRequest request) {

        log.warn("Agency not active: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Agency Not Active");
        problem.setType(URI.create("/errors/agency-not-active"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(InvalidAccountStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransition(
            InvalidAccountStateTransitionException ex, HttpServletRequest request) {

        log.warn("Invalid state transition: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Invalid State Transition");
        problem.setType(URI.create("/errors/invalid-state-transition"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Invalid argument at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Input");
        problem.setType(URI.create("/errors/invalid-input"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("Invalid operation at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Invalid Operation");
        problem.setType(URI.create("/errors/invalid-operation"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}", request.getRequestURI(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("/errors/internal-server-error"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
