package com.accenture.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration globale de la documentation OpenAPI.
 *
 * <p>🟢 Débutant : ce bean génère la page d'accueil de Swagger UI
 *     (titre, version, description).</p>
 * <p>🟡 Intermédiaire : on y déclare les schémas de sécurité réutilisables
 *     dans tout le contrat.</p>
 * <p>🔴 Avancé : on peut y ajouter des servers, extensions x-*, tags globaux,
 *     etc.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";
    private static final String BEARER_AUTH = "BearerAuth";

    @Bean
    public OpenAPI taskManagerOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())

                // Applique BearerAuth et basicAuth sur TOUS les endpoints par défaut
//                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
//                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))

                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Identifiants basiques (username / password)"))
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT — format : `Bearer <token>`")));
    }

    /**
     * Ajoute automatiquement une réponse 500 sur tous les endpoints.
     * Évite de répéter @ApiResponse(500) dans chaque interface.
     */
    @Bean
    public OpenApiCustomizer globalErrorResponses() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {
                    if (operation.getResponses() != null
                            && !operation.getResponses().containsKey("500")) {
                        operation.getResponses().addApiResponse("500",
                                new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("Erreur interne du serveur")
                                        .content(new io.swagger.v3.oas.models.media.Content()
                                                .addMediaType("application/json",
                                                        new io.swagger.v3.oas.models.media.MediaType()
                                                                .schema(new Schema<>()
                                                                        .$ref("#/components/schemas/ErrorDto"))))
                        );
                    }
                })
        );
    }

    // -------------------------------------------------------------------------
    // Informations générales
    // -------------------------------------------------------------------------

    private Info apiInfo() {
        return new Info()
                .title("Task Manager API")
                .version("1.0.0")
                .description("""
                        API REST de gestion de tâches.
                        
                        Permet de **créer**, **lire**, **modifier** et **supprimer** des tâches.
                        
                        Cette API est protégée : vous devez fournir
                        - un Bearer token
                        - ou des identifiants Basic pour accéder aux endpoints sécurisés.
                        """)
                .contact(new Contact()
                        .name("Équipe Accenture")
                        .email("dev@accenture.com")
                        .url("https://accenture.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    /**
     * Bean ObjectMapper partagé — gère la sérialisation JSON des dates (JavaTimeModule)
     * et est injecté dans {@code SecurityConfig} pour écrire les réponses d'erreur.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}
