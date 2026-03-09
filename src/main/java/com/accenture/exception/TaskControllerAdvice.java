package com.accenture.exception;

import com.accenture.dto.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Gestionnaire centralisé des exceptions pour toute l'API.
 *
 * <p>Résout les messages d'erreur via {@link MessageSource} selon la locale
 * de la requête (header {@code Accept-Language}).</p>
 */
@RestControllerAdvice
public class TaskControllerAdvice {

    private final MessageSource messageSource;

    public TaskControllerAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // -------------------------------------------------------------------------
    // Résolution i18n — méthode utilitaire
    // -------------------------------------------------------------------------

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    // =========================================================================
    // 1. Exception métier — TaskException
    // =========================================================================

    /**
     * Gère toutes les {@link TaskException} levées par les controllers.
     * Le code HTTP est porté par l'exception elle-même.
     */
    @ExceptionHandler(TaskException.class)
    public ResponseEntity<ErrorDto> handleTaskException(
            TaskException ex,
            HttpServletRequest request) {

        ErrorDto body = ErrorDto.of(
                ex.getStatus().value(),
                ex.getErrorCode(),
                msg(ex.getMessageKey(), ex.getMessageArgs()),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // =========================================================================
    // 2. Erreurs Spring MVC
    // =========================================================================

    /**
     * Corps JSON absent ou malformé → 400 BAD_REQUEST.
     * Ex : JSON invalide, body manquant sur un POST.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleNotReadable(
            HttpMessageNotReadableException ignored,
            HttpServletRequest request) {

        ErrorDto body = ErrorDto.of(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST_BODY",
                msg("error.request.body_unreadable"),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Paramètre de requête obligatoire manquant → 400 BAD_REQUEST.
     * Ex : {@code @RequestParam} sans {@code required = false}.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDto> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        ErrorDto body = ErrorDto.of(
                HttpStatus.BAD_REQUEST.value(),
                "MISSING_PARAMETER",
                msg("error.request.missing_param", ex.getParameterName()),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Type de paramètre incorrect → 400 BAD_REQUEST.
     * Ex : {@code /api/tasks/abc} alors qu'un {@code Long} est attendu.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String expected = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "?";

        ErrorDto body = ErrorDto.of(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PARAMETER_TYPE",
                msg("error.request.invalid_param_type", ex.getName(), expected),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(body);
    }

    // =========================================================================
    // 3. Accès refusé — 403 FORBIDDEN
    // =========================================================================

    /**
     * Levée par Spring Security quand {@code @PreAuthorize} échoue.
     * L'utilisateur est bien authentifié mais n'a pas le rôle requis.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(
            AccessDeniedException ignored,
            HttpServletRequest request) {

        ErrorDto body = ErrorDto.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                msg("error.security.access_denied"),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // =========================================================================
    // 4. Fallback général — 500 INTERNAL_SERVER_ERROR
    // =========================================================================

    /**
     * Capture toute exception non prévue → 500 INTERNAL_SERVER_ERROR.
     * Le message interne n'est PAS exposé (sécurité).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGeneric(
            Exception ignored,
            HttpServletRequest request) {

        ErrorDto body = ErrorDto.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                msg("error.internal"),
                request.getRequestURI()
        );

        return ResponseEntity.internalServerError().body(body);
    }
}
