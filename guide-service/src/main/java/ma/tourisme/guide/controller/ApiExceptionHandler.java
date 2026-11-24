package ma.tourisme.guide.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import ma.tourisme.guide.exception.NotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestion centralisée des exceptions du microservice Guide.
 *
 * <p>Les erreurs sont retournées au format JSON avec
 * un statut HTTP, un message et le chemin demandé.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Gère les ressources introuvables.
     *
     * @param exception exception de ressource introuvable
     * @param request requête HTTP
     * @return réponse HTTP 404
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(
            final NotFoundException exception,
            final HttpServletRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    /**
     * Gère les erreurs de validation des champs.
     *
     * @param exception exception de validation
     * @param request requête HTTP
     * @return réponse HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(
            final MethodArgumentNotValidException exception,
            final HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(field ->
                        field.getField()
                                + " : "
                                + field.getDefaultMessage()
                )
                .collect(Collectors.joining(", "));

        return build(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    /**
     * Gère les paramètres et corps de requête invalides.
     *
     * @param exception exception de validation
     * @param request requête HTTP
     * @return réponse HTTP 400
     */
    @ExceptionHandler({
        ConstraintViolationException.class,
        HandlerMethodValidationException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, Object>> badRequest(
            final Exception exception,
            final HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Parametres ou corps de requete invalides",
                request
        );
    }

    /**
     * Gère les exceptions portant un statut HTTP explicite.
     *
     * @param exception exception HTTP
     * @param request requête HTTP
     * @return réponse avec le statut correspondant
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(
            final ResponseStatusException exception,
            final HttpServletRequest request) {

        HttpStatus httpStatus = HttpStatus.valueOf(
                exception.getStatusCode().value()
        );

        String message = exception.getReason() != null
                ? exception.getReason()
                : httpStatus.getReasonPhrase();

        return build(
                httpStatus,
                message,
                request
        );
    }

    /**
     * Construit une réponse d'erreur standardisée.
     *
     * @param status statut HTTP de l'erreur
     * @param message description de l'erreur
     * @param request requête HTTP
     * @return réponse JSON structurée
     */
    private ResponseEntity<Map<String, Object>> build(
            final HttpStatus status,
            final String message,
            final HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
