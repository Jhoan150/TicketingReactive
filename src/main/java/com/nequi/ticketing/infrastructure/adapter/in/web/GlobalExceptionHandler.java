package com.nequi.ticketing.infrastructure.adapter.in.web;

import com.nequi.ticketing.domain.exception.DomainException;
import com.nequi.ticketing.domain.exception.EventNotFoundException;
import com.nequi.ticketing.domain.exception.OrderNotFoundException;
import com.nequi.ticketing.domain.exception.TicketNotAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.core.codec.DecodingException;
import reactor.core.publisher.Mono;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global reactive exception handler that maps domain exceptions to
 * RFC 7807 Problem Detail HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EventNotFoundException.class)
    public Mono<ProblemDetail> handleEventNotFound(EventNotFoundException ex) {
        return Mono.just(buildProblem(HttpStatus.NOT_FOUND, ex));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public Mono<ProblemDetail> handleOrderNotFound(OrderNotFoundException ex) {
        return Mono.just(buildProblem(HttpStatus.NOT_FOUND, ex));
    }

    @ExceptionHandler(TicketNotAvailableException.class)
    public Mono<ProblemDetail> handleTicketNotAvailable(TicketNotAvailableException ex) {
        return Mono.just(buildProblem(HttpStatus.CONFLICT, ex));
    }

    @ExceptionHandler(DomainException.class)
    public Mono<ProblemDetail> handleDomainException(DomainException ex) {
        return Mono.just(buildProblem(HttpStatus.BAD_REQUEST, ex));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidationException(WebExchangeBindException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setDetail(ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request"));
        problem.setProperty("timestamp", Instant.now().toString());
        return Mono.just(problem);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ProblemDetail> handleServerWebInputException(ServerWebInputException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getReason());
        problem.setProperty("timestamp", Instant.now().toString());
        return Mono.just(problem);
    }

    @ExceptionHandler(DecodingException.class)
    public Mono<ProblemDetail> handleDecodingException(DecodingException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("JSON Decoding Error");
        problem.setDetail("Malformed JSON request. Check for missing quotes or commas.");
        problem.setProperty("timestamp", Instant.now().toString());
        return Mono.just(problem);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unhandled exception processing request", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred");
        problem.setProperty("timestamp", Instant.now().toString());
        return Mono.just(problem);
    }

    private ProblemDetail buildProblem(HttpStatus status, DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(ex.getErrorCode());
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
