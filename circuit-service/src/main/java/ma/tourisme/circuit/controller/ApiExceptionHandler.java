package ma.tourisme.circuit.controller;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns every error of the Circuit API into the same JSON body ({@link ApiError}). */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  /** Error body: time, HTTP status, reason, readable message and request path. */
  public record ApiError(
      Instant timestamp, int status, String error, String message, String path) {}

  private ResponseEntity<Object> response(
      HttpStatusCode status, String message, HttpHeaders headers, WebRequest request) {
    String reason = HttpStatus.valueOf(status.value()).getReasonPhrase();
    String path = ((ServletWebRequest) request).getRequest().getRequestURI();
    return new ResponseEntity<>(
        new ApiError(Instant.now(), status.value(), reason, message, path), headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String message = HttpStatus.valueOf(status.value()).getReasonPhrase();
    if (ex instanceof ResponseStatusException error && error.getReason() != null) {
      message = error.getReason();
    }
    if (ex instanceof MethodArgumentNotValidException) {
      message = "Données du circuit invalides";
    }
    return response(status, message, headers, request);
  }

  /** Any unexpected failure: logged on the server, generic 500 for the client. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> unexpected(Exception ex, WebRequest request) {
    LOG.error("Unexpected Circuit failure", ex);
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Erreur interne du service Circuit",
        new HttpHeaders(),
        request);
  }
}
