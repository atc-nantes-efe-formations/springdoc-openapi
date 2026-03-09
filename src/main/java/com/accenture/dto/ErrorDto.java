package com.accenture.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de réponse en cas d'erreur.
 *
 * <p>Retourné par le {@code TaskControllerAdvice} pour tous les codes 4xx / 5xx.
 * Toujours au format JSON, quel que soit le type d'erreur.</p>
 *
 * @param status    Code HTTP numérique (ex : 404)
 * @param errorCode Code métier lisible par la machine (ex : TASK_NOT_FOUND)
 * @param message   Message lisible par un humain
 * @param path      Chemin de la requête qui a déclenché l'erreur
 * @param timestamp Horodatage de l'erreur (ISO-8601)
 */
@Schema(
        name = "ErrorDto",
        description = "Corps de réponse standard pour toutes les erreurs de l'API"
)
public record ErrorDto(

        @Schema(
                description = "Code HTTP de la réponse",
                examples = {"404"}
        )
        int status,

        @Schema(
                description = "Code d'erreur métier identifiant le type de problème",
                examples = {"TASK_NOT_FOUND"}
        )
        String errorCode,

        @Schema(
                description = "Message explicatif destiné au développeur ou à l'utilisateur",
                examples = {"Tâche introuvable avec l'identifiant : 42"}
        )
        String message,

        @Schema(
                description = "Chemin HTTP de la requête ayant provoqué l'erreur",
                examples = {"/api/tasks/42"}
        )
        String path,

        @Schema(
                description = "Horodatage de l'erreur au format ISO-8601",
                examples = {"2026-03-09T14:32:00"},
                type = "string",
                format = "date-time"
        )
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp

) {
    // -------------------------------------------------------------------------
    // Fabrique statique — simplifie la création dans le ControllerAdvice
    // -------------------------------------------------------------------------

    public static ErrorDto of(int status, String errorCode, String message, String path) {
        return new ErrorDto(status, errorCode, message, path, LocalDateTime.now());
    }
}

