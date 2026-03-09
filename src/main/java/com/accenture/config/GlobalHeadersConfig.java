package com.accenture.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

/**
 * Personnalisation globale des opérations OpenAPI.
 *
 * <p>🔴 Avancé : {@link OperationCustomizer} permet d'injecter automatiquement
 * des paramètres (headers, query params) sur <strong>tous</strong> les endpoints
 * sans les répéter manuellement dans chaque interface.</p>
 *
 * <p>Cas d'usage typiques :</p>
 * <ul>
 *   <li>Header {@code Accept-Language} d'internationalisation</li>
 *   <li>Header de version d'API</li>
 * </ul>
 */
@Configuration
public class GlobalHeadersConfig {

    @Bean
    public OperationCustomizer globalHeaders() {
        return (Operation operation, HandlerMethod ignored) -> {
            operation.addParametersItem(acceptLanguageHeader());
            return operation;
        };
    }

    // -------------------------------------------------------------------------
    // Définitions des headers globaux
    // -------------------------------------------------------------------------

    private Parameter acceptLanguageHeader() {
        return new Parameter()
                .in("header")
                .name("Accept-Language")
                .description("Langue préférée pour les messages de réponse (ex: fr, en, es)")
                .required(false)
                .example("fr")
                .schema(new io.swagger.v3.oas.models.media.StringSchema()
                        ._default("fr"));
    }
}
