package com.accenture.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO en sortie représentant une tâche persistée.
 *
 * @param id          Identifiant unique de la tâche
 * @param title       Titre de la tâche
 * @param description Description détaillée
 * @param done        Statut d'achèvement
 */
@Schema(
        name = "TaskResponse",
        description = "Représentation d'une tâche retournée par l'API"
)
public record TaskResponse(

        @Schema(
                description = "Identifiant unique de la tâche",
                examples = {"1", "42"},
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Long id,

        @Schema(
                description = "Titre de la tâche",
                examples = {
                        "Préparer la présentation OpenAPI",
                        "Rédiger la documentation de l'API"
                }
        )
        String title,

        @Schema(
                description = "Description détaillée de la tâche",
                examples = {
                        "Couvrir les sections théorie, installation et démo live",
                        "Inclure des exemples de requêtes et de réponses"
                },
                nullable = true
        )
        String description,

        @Schema(
                description = "La tâche est-elle terminée ?",
                examples = {
                        "false",
                        "true"
                }
        )
        boolean done

) {}

