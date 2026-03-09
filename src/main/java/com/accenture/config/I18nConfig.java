package com.accenture.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Configuration de l'internationalisation (i18n).
 *
 * <p>La locale est déterminée automatiquement à partir du header HTTP
 * {@code Accept-Language} de chaque requête.</p>
 *
 * <p>Langues supportées : français (défaut), anglais, espagnol.</p>
 */
@Configuration
public class I18nConfig {

    /**
     * Résout la locale à partir du header {@code Accept-Language}.
     * Repli sur {@code fr} si la langue demandée n'est pas supportée.
     */
    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(
                Locale.FRENCH,
                Locale.ENGLISH,
                Locale.forLanguageTag("es")
        ));
        resolver.setDefaultLocale(Locale.FRENCH);
        return resolver;
    }

    /**
     * Source des messages externalisés.
     * Charge les fichiers {@code messages_fr.properties}, {@code messages_en.properties},
     * {@code messages_es.properties} depuis le classpath.
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(Locale.FRENCH);
        source.setFallbackToSystemLocale(false);
        return source;
    }
}

