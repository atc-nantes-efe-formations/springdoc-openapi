package com.accenture.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO en entrée pour la création ou la mise à jour d'une tâche.
 *
 * @param title       Titre de la tâche (obligatoire)
 * @param description Description détaillée (optionnelle)
 * @param done        Statut d'achèvement
 */
@Schema(
        name = "TaskRequest",
        description = "Corps de la requête pour créer ou modifier une tâche"
)
public record TaskRequest(

        @Schema(
                description = "Titre de la tâche",
                examples = {
                        "Préparer la présentation OpenAPI",
                        "Faire les courses",
                        "Appeler le client"
                },
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 100
        )
        String title,

        @Schema(
                description = "Description détaillée de la tâche",
                examples = {
                        "Couvrir les sections théorie",
                        "installation",
                        "démo live"
                },
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true
        )
        String description,

        @Schema(
                description = "La tâche est-elle terminée ?",
                examples = {
                        "true",
                        "false"
                },
                defaultValue = "false"
        )
        boolean done

) {}

