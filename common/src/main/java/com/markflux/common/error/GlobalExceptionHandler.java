package com.markflux.common.error;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ObjectProvider<Tracer> tracer;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.warn(ex.getMessage(), ex);
        }
        ProblemDetail problem = problem(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
        ex.getDetails().forEach(problem::setProperty);
        return response(problem, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return response(problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid access token is required."),
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return response(problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to perform this operation."), request);
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Object> handleConcurrentUpdate(ConcurrencyFailureException ex, WebRequest request) {
        return response(problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was changed by another request. Please retry."), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.info("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return response(problem(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "The request conflicts with existing data."), request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Object> handleInvalidSort(PropertyReferenceException ex, WebRequest request) {
        return response(problem(HttpStatus.BAD_REQUEST, "INVALID_SORT_PROPERTY",
                "Unknown sort property: " + ex.getPropertyName()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return response(problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred."), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Violation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new Violation(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "The request contains invalid fields.");
        problem.setProperty("violations", violations);
        return response(problem, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            addCommonFields(problem, statusCode, request);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<Object> response(ProblemDetail problem, WebRequest request) {
        return createResponseEntity(problem, new HttpHeaders(), HttpStatusCode.valueOf(problem.getStatus()), request);
    }

    private void addCommonFields(ProblemDetail problem, HttpStatusCode statusCode, WebRequest request) {
        if (problem.getProperties() == null || !problem.getProperties().containsKey("errorCode")) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            problem.setProperty("errorCode", status != null ? status.name() : "ERROR");
        }
        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        problem.setProperty("timestamp", Instant.now());
        Span span = tracer.getIfAvailable(() -> Tracer.NOOP).currentSpan();
        if (span != null) {
            problem.setProperty("traceId", span.context().traceId());
        }
    }

    private static ProblemDetail problem(HttpStatus status, String errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorCode", errorCode);
        return problem;
    }

    public record Violation(String field, String message) {
    }
}
