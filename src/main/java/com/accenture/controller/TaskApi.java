package com.accenture.controller;

import org.springframework.web.bind.annotation.*;
import com.accenture.dto.ErrorDto;
import com.accenture.dto.TaskRequest;
import com.accenture.dto.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Contrat REST de l'API Tâches.
 *
 * <p>🟡 Intermédiaire : toutes les annotations OpenAPI sont ici, dans l'interface.
 * L'implémentation ({@code TaskController}) reste propre, sans aucune annotation de doc.</p>
 *
 * <p>🔴 Avancé : ce pattern "interface-first" permet de versionner le contrat
 * indépendamment de l'implémentation et de générer un SDK client à partir du JSON OpenAPI.</p>
 */
@Tag(
        name = "Tâches",
        description = "Opérations CRUD sur les tâches de l'utilisateur connecté"
)
public interface TaskApi {

    /**
     * GET /api/tasks — Liste toutes les tâches.
     *
     * @param done filtre par statut d'achèvement (optionnel)
     * @return liste des tâches
     */
    @Operation(
            summary = "Lister toutes les tâches",
            description = """
                    Retourne la liste complète des tâches de l'utilisateur connecté.
                    
                    Le paramètre `done` permet de filtrer par statut d'achèvement.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Liste récupérée avec succès",
            content = @Content(
                    mediaType = "application/json",
                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                            schema = @Schema(implementation = TaskResponse.class)
                    )
            )
    )
    ResponseEntity<List<TaskResponse>> getAllTasks(

            @Parameter(
                    name = "done",
                    description = "Filtre par statut : `true` = terminées, `false` = en cours, absent = toutes",
                    in = ParameterIn.QUERY,
                    required = false,
                    example = "false"
            )
            @RequestParam(required = false) Boolean done
    );

    /**
     * GET /api/tasks/{id} — Récupère une tâche par son ID.
     *
     * @param id identifiant de la tâche
     * @return tâche correspondante
     */
    @Operation(
            summary = "Récupérer une tâche",
            description = "Retourne les détails d'une tâche identifiée par son ID."
    )

    @ApiResponse(
            responseCode = "200",
            description = "Tâche trouvée",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class),
                    examples = @ExampleObject(
                            name = "Exemple",
                            value = """
                                    {
                                      "id": 42,
                                      "title": "Préparer la présentation OpenAPI",
                                      "description": "Couvrir théorie, installation et démo live",
                                      "done": false
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tâche introuvable",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)
            )
    )

    ResponseEntity<TaskResponse> getTaskById(

            @Parameter(
                    name = "id",
                    description = "Identifiant unique de la tâche",
                    in = ParameterIn.PATH,
                    required = true,
                    example = "42"
            )
            @PathVariable Long id
    );

    /**
     * POST /api/tasks — Créer une tâche.
     *
     * @param request données de la nouvelle tâche
     * @return tâche créée
     */
    @Operation(
            summary = "Créer une tâche",
            description = "Crée une nouvelle tâche et la retourne avec son ID généré.",
            security = {
                    @SecurityRequirement(name = "basicAuth"),
                    @SecurityRequirement(name = "BearerAuth")
            }
    )

    @ApiResponse(
            responseCode = "201",
            description = "Tâche créée avec succès",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Corps de la requête invalide (champs manquants ou mal formés)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Non authentifié",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Accès refusé — rôle USER ou ADMIN requis",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)
            )
    )
    ResponseEntity<TaskResponse> createTask(

            @RequestBody(
                    description = "Données de la nouvelle tâche",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskRequest.class),
                            examples = @ExampleObject(
                                    name = "Nouvelle tâche",
                                    value = """
                                            {
                                              "title": "Préparer la présentation OpenAPI",
                                              "description": "Couvrir théorie, installation et démo live",
                                              "done": false
                                            }
                                            """
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody TaskRequest request
    );

    /**
     * PUT /api/tasks/{id} — Mettre à jour une tâche.
     *
     * @param id      identifiant de la tâche
     * @param request nouvelles données de la tâche
     * @return tâche mise à jour
     */
    @Operation(
            summary = "Mettre à jour une tâche",
            description = "Remplace intégralement les données d'une tâche existante.",
            security = {
                    @SecurityRequirement(name = "basicAuth"),
                    @SecurityRequirement(name = "BearerAuth")
            }
    )

    @ApiResponse(responseCode = "200", description = "Tâche mise à jour",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)))
    @ApiResponse(responseCode = "404", description = "Tâche introuvable",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)))
    @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)))
    @ApiResponse(responseCode = "403", description = "Accès refusé — rôle USER ou ADMIN requis",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)))

    ResponseEntity<TaskResponse> updateTask(

            @Parameter(name = "id", description = "ID de la tâche à modifier",
                    in = ParameterIn.PATH, required = true, example = "42")
            @PathVariable Long id,

            @org.springframework.web.bind.annotation.RequestBody TaskRequest request
    );

    /**
     * DELETE /api/tasks/{id} — Supprimer une tâche.
     *
     * @param id identifiant de la tâche
     * @return aucune valeur
     */
    @Operation(
            summary = "Supprimer une tâche",
            description = "Supprime définitivement une tâche. Cette action est irréversible.",
            security = {
                    @SecurityRequirement(name = "basicAuth"),
                    @SecurityRequirement(name = "BearerAuth")
            }
    )

    @ApiResponse(responseCode = "204", description = "Tâche supprimée (pas de contenu)",
            content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "Tâche introuvable",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)))
    @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)))
    @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)))

    ResponseEntity<Void> deleteTask(

            @Parameter(name = "id", description = "ID de la tâche à supprimer",
                    in = ParameterIn.PATH, required = true, example = "42")
            @PathVariable Long id
    );
}
