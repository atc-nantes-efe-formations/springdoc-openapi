package com.accenture.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception métier centralisée pour l'API Tâches.
 *
 * <p>Porte une <strong>clé i18n</strong> et des arguments optionnels
 * plutôt qu'un message hardcodé. Le message final est résolu dans le
 * {@code TaskControllerAdvice} selon la locale de la requête.</p>
 *
 * <p>Exemples d'utilisation :</p>
 * <pre>{@code
 * throw TaskException.notFound(42L);
 * throw TaskException.badRequest("error.task.title_blank");
 * }</pre>
 */
public class TaskException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String messageKey;
    private final Object[] messageArgs;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public TaskException(HttpStatus status, String errorCode, String messageKey, Object... messageArgs) {
        super(messageKey);   // message brut = la clé (utile pour les logs)
        this.status      = status;
        this.errorCode   = errorCode;
        this.messageKey  = messageKey;
        this.messageArgs = messageArgs;
    }

    // -------------------------------------------------------------------------
    // Fabriques statiques
    // -------------------------------------------------------------------------

    public static TaskException notFound(Long id) {
        return new TaskException(
                HttpStatus.NOT_FOUND,
                "TASK_NOT_FOUND",
                "error.task.not_found",
                id
        );
    }

    public static TaskException badRequest(String messageKey, Object... args) {
        return new TaskException(
                HttpStatus.BAD_REQUEST,
                "TASK_BAD_REQUEST",
                messageKey,
                args
        );
    }

    public static TaskException conflict(String messageKey, Object... args) {
        return new TaskException(
                HttpStatus.CONFLICT,
                "TASK_CONFLICT",
                messageKey,
                args
        );
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public HttpStatus getStatus()      { return status; }
    public String getErrorCode()       { return errorCode; }
    public String getMessageKey()      { return messageKey; }
    public Object[] getMessageArgs()   { return messageArgs; }
}
