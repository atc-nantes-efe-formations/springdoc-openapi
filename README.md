# Support de cours — OpenAPI & Swagger avec Spring Boot

> **Stack** : Spring Boot 4.0.3 · Java 25 · springdoc-openapi 3.0.2  
> **Projet démo** : `Task Manager API` — CRUD de tâches en mémoire  
> **Légende des niveaux** :  
> 🟢 **Débutant** — comprendre à quoi ça sert  
> 🟡 **Intermédiaire** — comprendre comment ça fonctionne  
> 🔴 **Avancé** — aller plus loin, bonnes pratiques

---

## Table des matières

1. [Qu'est-ce que c'est, à quoi ça sert ?](#1-quest-ce-que-cest--à-quoi-ça-sert-)
2. [Où ça vit dans l'architecture](#2-où-ça-vit-dans-larchitecture)
3. [Installation minimale](#3-installation-minimale)
4. [Configuration globale](#4-configuration-globale)
5. [Contrat REST via interface](#5-contrat-rest-via-interface)
6. [Annotations OpenAPI côté endpoints](#6-annotations-openapi-côté-endpoints)
7. [Documentation des DTO](#7-documentation-des-dto)
8. [Headers HTTP](#8-headers-http)
9. [Authentification](#9-authentification)
10. [Gestion des erreurs](#10-gestion-des-erreurs)
11. [Bonnes pratiques & erreurs fréquentes](#11-bonnes-pratiques--erreurs-fréquentes)
12. [Le fichier OpenAPI — JSON, YAML & génération de code](#12-le-fichier-openapi--json-yaml--génération-de-code)
13. [Conclusion](#13-conclusion)

---

## 1. Qu'est-ce que c'est ? À quoi ça sert ?

### 🟢 OpenAPI — le contrat

**OpenAPI** (anciennement *Swagger Specification*) est une **spécification standard** pour décrire une API REST sous forme de fichier structuré (JSON ou YAML).

> Pensez-y comme un **plan d'architecte** pour votre API : il décrit tous les endpoints, les paramètres attendus, les réponses possibles et les règles de sécurité — indépendamment de tout langage de programmation.

Ce fichier est lisible par des humains **et** par des machines.

```
Votre API Spring Boot
       │
       ▼
  springdoc-openapi    ──►  /v3/api-docs  (JSON brut)
       │
       ▼
   Swagger UI          ──►  /swagger-ui.html  (interface web)
```

### 🟢 Swagger UI — l'interface

**Swagger UI** est une **interface web** générée automatiquement qui permet de :

- 📖 **Lire** la documentation de l'API
- 🧪 **Tester** les endpoints directement dans le navigateur (sans Postman)
- 🤝 **Partager** le contrat avec les équipes front-end, QA, partenaires

### 🟢 springdoc — l'intégration Spring Boot

**springdoc-openapi** est la bibliothèque qui fait le pont entre votre code Spring Boot et la spec OpenAPI. Elle :

1. Inspecte vos controllers au démarrage de l'application
2. Génère automatiquement le fichier JSON OpenAPI
3. Sert l'interface Swagger UI

### 🟢 Cas d'usage concrets

| Situation | Apport d'OpenAPI |
|-----------|-----------------|
| Équipe front séparée du back | Contrat partagé, pas besoin d'attendre l'implémentation |
| Onboarding d'un nouveau développeur | Documentation toujours à jour, testable en 5 minutes |
| Intégration avec des partenaires | Génération de SDK client automatique |
| Tests d'intégration | Validation du contrat sans déploiement |
| API publique | Documentation officielle pour les tiers |

### 🟡 Code First vs Design First

| Approche | Description | Avantage |
|----------|-------------|----------|
| **Code First** | On code, OpenAPI est généré | Rapide à démarrer |
| **Design First** | On écrit le YAML d'abord, puis on génère le squelette | Meilleur alignement équipes |

> springdoc supporte les deux, mais en contexte Spring Boot on pratique majoritairement le **Code First**.

### 🔴 Limites à connaître

- ❌ OpenAPI **ne valide pas** la logique métier
- ❌ OpenAPI **ne remplace pas** les tests unitaires
- ❌ Des annotations mal placées peuvent **polluer le domaine**
- ❌ La doc peut **mentir** si on ne la maintient pas (annotations obsolètes)
- ❌ Swagger UI **ne doit pas être exposé** en production sans protection

---

## 2. Où ça vit dans l'architecture

### 🟢 Vue d'ensemble

OpenAPI vit **exclusivement dans la couche adaptateur REST**. C'est la règle d'or.

```
┌───────────────────────────────────────────────────────────────────────┐
│                     COUCHE PRÉSENTATION / REST                        │
│                                                                       │
│  TaskApi.java           ◄── Annotations @Tag, @Operation, etc.        │
│  TaskController.java    ◄── Délégation au service (sans doc)          │
│  OpenApiConfig.java     ◄── Configuration globale OpenAPI             │
│  GlobalHeadersConfig    ◄── Headers transverses (OperationCustomizer) │
│  I18nConfig.java        ◄── LocaleResolver + MessageSource            │
│  SecurityConfig.java    ◄── Règles d'accès + filtre Bearer            │
│  TaskRequest/Response   ◄── @Schema sur les DTO                       │
│  ErrorDto               ◄── @Schema sur le DTO d'erreur               │
│                                                                       │
├───────────────────────────────────────────────────────────────────────┤
│                       COUCHE SERVICE                                  │
│                                                                       │
│  TaskService.java       ◄── Logique métier + validation               │
│                         ◄── Lève des TaskException (clés i18n)        │
│                         ◄── 🚫 AUCUNE annotation OpenAPI              │
│                                                                       │
├───────────────────────────────────────────────────────────────────────┤
│                       COUCHE DOMAINE                                  │
│                                                                       │
│  Task.java (entité)     ◄── 🚫 AUCUNE annotation OpenAPI              │
│  TaskRepository.java    ◄── 🚫 AUCUNE annotation OpenAPI              │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

### 🟡 Règle de placement

| Fichier | Annotations OpenAPI autorisées |
|---------|-------------------------------|
| `TaskApi.java` (interface) | ✅ Toutes : `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`... |
| `TaskController.java` (impl) | ✅ `@RestController`, `@RequestMapping`, `@GetMapping`... (mapping HTTP) |
| `TaskService.java` | ❌ Interdit — couche service |
| `TaskRequest/Response/ErrorDto` (DTO) | ✅ `@Schema` uniquement |
| `OpenApiConfig.java` (config) | ✅ Configuration programmatique OpenAPI |
| `I18nConfig.java` (config) | ❌ Aucune — config technique pure |
| `SecurityConfig.java` (config) | ❌ Aucune — config technique pure |
| `Task.java` (entité JPA) | ❌ Interdit — couche domaine |

### 🔴 Architecture hexagonale

Dans une architecture hexagonale (ports & adapters) :

```
  [Swagger UI]  ──►  [TaskApi / TaskController]  ──►  [TaskService]
                           (adaptateur primaire)              │
                                                       [Port TaskRepository]
                                                              │
                                                     [TaskRepositoryAdapter]
```

La documentation OpenAPI appartient **à l'adaptateur primaire**, jamais au service ou au domaine.

---

## 3. Installation minimale

### 🟢 Dépendance Maven

Une seule dépendance suffit pour avoir Swagger UI dans votre projet :

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.2</version>
</dependency>
```

> ✅ **C'est tout.** springdoc détecte Spring Boot automatiquement via l'auto-configuration.

### 🟢 URLs disponibles après démarrage

| URL | Contenu |
|-----|---------|
| `http://localhost:8080/swagger-ui.html` | Interface Swagger UI |
| `http://localhost:8080/v3/api-docs` | Spécification JSON OpenAPI |
| `http://localhost:8080/v3/api-docs.yaml` | Spécification YAML OpenAPI |

### 🟡 Propriétés de configuration (application.properties)

```properties
# Chemin de l'interface Swagger UI (défaut : /swagger-ui/index.html)
springdoc.swagger-ui.path=/swagger-ui.html

# Chemin du JSON OpenAPI brut (défaut : /v3/api-docs)
springdoc.api-docs.path=/v3/api-docs

# Tri alphabétique des endpoints et des tags
springdoc.swagger-ui.operations-sorter=alpha
springdoc.swagger-ui.tags-sorter=alpha

# En production : décommenter ces lignes pour désactiver Swagger UI
# springdoc.swagger-ui.enabled=false
# springdoc.api-docs.enabled=false
```

### 🔴 Sans aucune annotation, que génère springdoc ?

springdoc inspecte les annotations Spring MVC (`@GetMapping`, `@PostMapping`, etc.) et génère un contrat **basique mais fonctionnel**. Les annotations OpenAPI enrichissent ensuite ce contrat avec des descriptions, exemples et règles de sécurité.

---

## 4. Configuration globale

### 🟢 Bean `OpenAPI`

Créez une classe de configuration pour personnaliser les métadonnées de votre API :

```java
// src/main/java/com/accenture/config/OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskManagerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Manager API")
                        .version("1.0.0")
                        .description("API REST de gestion de tâches.")
                        .contact(new Contact()
                                .name("Équipe Accenture")
                                .email("dev@accenture.com"))
                );
    }
}
```

> 🟢 Ce bean s'affiche dans l'en-tête de Swagger UI.

### 🟡 Description enrichie avec Markdown

```java
.description("""
        API REST de gestion de tâches.
        
        Permet de **créer**, **lire**, **modifier** et **supprimer** des tâches.
        
        > Cette API est protégée par un Bearer token JWT.
        """)
```

> Swagger UI rend le Markdown nativement.

### 🟡 Informations complètes

```java
new Info()
    .title("Task Manager API")
    .version("1.0.0")
    .description("...")
    .contact(new Contact()
        .name("Équipe Accenture")
        .email("dev@accenture.com")
        .url("https://accenture.com"))
    .license(new License()
        .name("Apache 2.0")
        .url("https://www.apache.org/licenses/LICENSE-2.0"))
```

### 🔴 Déclaration des schémas de sécurité

Les schémas de sécurité sont déclarés une fois dans la config, puis référencés dans chaque endpoint via `@SecurityRequirement`.

> 💡 Bonne pratique : déclarer les noms en **constantes** pour éviter les fautes de frappe :

```java
private static final String BASIC_AUTH  = "basicAuth";
private static final String BEARER_AUTH = "BearerAuth";
```

```java
@Bean
public OpenAPI taskManagerOpenAPI() {
    return new OpenAPI()
            .info(apiInfo())
            // Active les deux schémas globalement sur toute l'API
            .addSecurityItem(new SecurityRequirement()
                    .addList(BASIC_AUTH)
                    .addList(BEARER_AUTH))
            .components(new Components()
                    // Basic Auth — Authorization: Basic dXNlcjpwYXNz
                    .addSecuritySchemes(BASIC_AUTH,
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("basic")
                                    .description("Identifiants basiques (username / password)"))
                    // Bearer token — Authorization: Bearer <token>
                    .addSecuritySchemes(BEARER_AUTH,
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("Token Bearer — format : `Bearer <token>`"))
            );
}
```

### 🔴 Injecter une réponse 500 sur tous les endpoints automatiquement

Plutôt que de répéter `@ApiResponse(responseCode = "500", ...)` sur chaque méthode de l'interface, un `OpenApiCustomizer` permet de l'ajouter une seule fois pour toute l'API :

```java
// dans OpenApiConfig.java
@Bean
public OpenApiCustomizer globalErrorResponses() {
    return openApi -> openApi.getPaths().values().forEach(pathItem ->
            pathItem.readOperations().forEach(operation -> {
                if (!operation.getResponses().containsKey("500")) {
                    operation.getResponses().addApiResponse("500",
                            new ApiResponse()
                                    .description("Erreur interne du serveur")
                                    .content(new Content()
                                            .addMediaType("application/json",
                                                    new MediaType().schema(
                                                            new Schema<>().$ref("#/components/schemas/ErrorDto")
                                                    )))
                    );
                }
            })
    );
}
```

> 🔴 Le schéma `ErrorDto` est réutilisable car il est enregistré dans les `Components` via `addSchemas("ErrorDto", ...)`.

---

## 5. Contrat REST via interface

### 🟡 Le pattern "interface-first"

La meilleure pratique Spring Boot / OpenAPI est de **séparer le contrat de l'implémentation** :

```
TaskApi.java          ← Toutes les annotations OpenAPI + Spring MVC
    ▲
    │ implements
TaskController.java   ← Délégation pure au service, aucune annotation de doc
    │ utilise
TaskService.java      ← Logique métier + validation, aucune annotation OpenAPI
```

**Avantages :**
- Le contrat est lisible d'un seul coup d'œil
- L'implémentation reste propre
- La logique est testable indépendamment du transport HTTP
- On peut changer l'implémentation sans toucher au contrat

### 🟡 Interface annotée

```java
// TaskApi.java — le contrat
@Tag(name = "Tâches", description = "Opérations CRUD sur les tâches")
public interface TaskApi {

    @Operation(summary = "Lister toutes les tâches")
    ResponseEntity<List<TaskResponse>> getAllTasks(@RequestParam(required = false) Boolean done);

    @Operation(summary = "Créer une tâche")
    ResponseEntity<TaskResponse> createTask(@RequestBody TaskRequest request);
    
    // ...
}
```

### 🟡 Controller — délégation pure

```java
// TaskController.java — mapping HTTP ici, aucune annotation OpenAPI
@RestController
@RequestMapping("/api/tasks")
public class TaskController implements TaskApi {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(Boolean done) {
        return ResponseEntity.ok(taskService.findAll(done));
    }

    @Override
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(TaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }
}
```

### 🟡 Service — logique + validation

```java
// TaskService.java — tout ce qui n'est pas HTTP
@Service
public class TaskService {

    public TaskResponse create(TaskRequest request) {
        validate(request);        // validation ici, pas dans le controller
        // ... persistence
        return new TaskResponse(...);
    }

    private void validate(TaskRequest request) {
        if (request == null) {
            throw TaskException.badRequest("error.task.body_required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw TaskException.badRequest("error.task.title_blank");
        }
        if (request.title().strip().length() > 100) {
            throw TaskException.badRequest("error.task.title_too_long");
        }
        if (request.description() != null && request.description().length() > 500) {
            throw TaskException.badRequest("error.task.description_too_long");
        }
    }
}
```

### 🔴 Versioning du contrat

```
src/main/java/com/accenture/controller/
├── v1/
│   ├── TaskApiV1.java        ← Contrat v1
│   └── impl/
│       └── TaskControllerV1.java
└── v2/
    ├── TaskApiV2.java        ← Contrat v2 (enrichi)
    └── impl/
        └── TaskControllerV2.java
```

---

## 6. Annotations OpenAPI côté endpoints

### 🟢 `@Tag` — Regrouper les endpoints

```java
@Tag(
    name = "Tâches",
    description = "Opérations CRUD sur les tâches de l'utilisateur connecté"
)
@RequestMapping("/api/tasks")
public interface TaskApi { ... }
```

> `@Tag` sur l'interface regroupe tous les endpoints sous le même onglet dans Swagger UI.

### 🟢 `@Operation` — Décrire un endpoint

```java
@Operation(
    summary = "Créer une tâche",           // titre court (visible dans la liste)
    description = "Crée une nouvelle tâche et retourne l'objet créé avec son ID."
    // description longue (visible au déplié)
)
@PostMapping
ResponseEntity<TaskResponse> createTask(...);
```

### 🟡 `@ApiResponse` / `@ApiResponses` — Documenter les réponses

```java
@ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Tâche créée avec succès",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = TaskResponse.class)
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "Corps de requête invalide",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorDto.class)  // ← le vrai DTO d'erreur
        )
    )
})
```

> Documenter **aussi les erreurs** avec le vrai schéma `ErrorDto` est une bonne pratique essentielle.

### 🟡 `@Parameter` — Décrire les paramètres

```java
// Path parameter
@Parameter(
    name = "id",
    description = "Identifiant unique de la tâche",
    in = ParameterIn.PATH,    // PATH, QUERY, HEADER, COOKIE
    required = true,
    example = "42"
)
@PathVariable Long id

// Query parameter
@Parameter(
    name = "done",
    description = "Filtre : true = terminées, false = en cours, absent = toutes",
    in = ParameterIn.QUERY,
    required = false,
    example = "false"
)
@RequestParam(required = false) Boolean done
```

### 🟡 `@RequestBody` OpenAPI — Décrire le corps de la requête

> Attention : il y a **deux** `@RequestBody` :
> - `io.swagger.v3.oas.annotations.parameters.RequestBody` → pour la documentation
> - `org.springframework.web.bind.annotation.RequestBody` → pour Spring MVC

```java
@RequestBody(                          // ← annotation OpenAPI (documentation)
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
@org.springframework.web.bind.annotation.RequestBody TaskRequest request  // ← Spring MVC
```

### 🔴 `@SecurityRequirement` — Sécuriser un endpoint

```java
@Operation(
    summary = "Lister toutes les tâches",
    security = @SecurityRequirement(name = "BearerAuth")  // référence le schéma déclaré dans OpenApiConfig
)
@GetMapping
ResponseEntity<List<TaskResponse>> getAllTasks(...);
```

> Sur les endpoints d'écriture (POST, PUT, DELETE), les deux schémas sont listés — l'un **ou** l'autre suffit :

```java
@Operation(
    summary = "Supprimer une tâche",
    security = {
        @SecurityRequirement(name = "basicAuth"),
        @SecurityRequirement(name = "BearerAuth")
    }
)
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
ResponseEntity<Void> deleteTask(@PathVariable Long id);
```

> ⚠️ `security = {}` sur `@Operation` **remplace** le `addSecurityItem` global pour cet endpoint. Il faut donc lister **tous** les schémas acceptés.

### 🔴 `@Schema(hidden = true)` — Masquer le body d'une réponse 204

Pour un `DELETE` qui retourne `204 No Content`, on masque explicitement le body dans Swagger UI :

```java
@ApiResponse(
    responseCode = "204",
    description = "Tâche supprimée (pas de contenu)",
    content = @Content(schema = @Schema(hidden = true))
)
```

### 🔴 `@ExampleObject` — Fournir des exemples nommés

```java
@ApiResponse(
    responseCode = "200",
    content = @Content(
        examples = {
            @ExampleObject(name = "Tâche simple",    value = "{\"id\":1,\"title\":\"Faire les courses\",\"done\":false}"),
            @ExampleObject(name = "Tâche terminée",  value = "{\"id\":2,\"title\":\"Préparer la démo\",\"done\":true}")
        }
    )
)
```

---

## 7. Documentation des DTO

### 🟢 `@Schema` sur un record Java

Les **records** Java sont parfaits pour les DTO : immutables, concis, et bien supportés par springdoc.

```java
@Schema(
    name = "TaskRequest",
    description = "Corps de la requête pour créer ou modifier une tâche"
)
public record TaskRequest(
    String title,
    String description,
    boolean done
) {}
```

### 🟡 `@Schema` sur les champs

```java
public record TaskRequest(

    @Schema(
        description = "Titre de la tâche",
        examples = {"Préparer la présentation OpenAPI", "Faire les courses"},
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 1,
        maxLength = 100
    )
    String title,

    @Schema(
        description = "Description détaillée (optionnelle)",
        examples = {"Couvrir théorie, installation et démo"},
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true
    )
    String description,

    @Schema(
        description = "La tâche est-elle terminée ?",
        examples = {"false", "true"},
        defaultValue = "false"
    )
    boolean done

) {}
```

### 🟡 DTO de réponse — `accessMode`

```java
public record TaskResponse(

    @Schema(
        description = "ID généré par le serveur",
        examples = {"1", "42"},
        accessMode = Schema.AccessMode.READ_ONLY  // n'apparaît pas dans le formulaire de création
    )
    Long id,

    @Schema(
        description = "Titre de la tâche",
        examples = {"Préparer la présentation OpenAPI", "Rédiger la documentation de l'API"}
    )
    String title,

    @Schema(
        description = "Description détaillée de la tâche",
        examples = {"Couvrir les sections théorie, installation et démo live"},
        nullable = true
    )
    String description,

    @Schema(
        description = "La tâche est-elle terminée ?",
        examples = {"false", "true"}
    )
    boolean done
) {}
```

### 🟡 DTO d'erreur — `ErrorDto`

Toutes les erreurs de l'API (4xx, 5xx) partagent **le même format** grâce à `ErrorDto` :

```java
@Schema(name = "ErrorDto", description = "Corps de réponse standard pour toutes les erreurs de l'API")
public record ErrorDto(

    @Schema(description = "Code HTTP de la réponse", examples = {"404"})
    int status,

    @Schema(description = "Code d'erreur métier", examples = {"TASK_NOT_FOUND"})
    String errorCode,

    @Schema(description = "Message explicatif", examples = {"Tâche introuvable avec l'identifiant : 42"})
    String message,

    @Schema(description = "Chemin HTTP de la requête", examples = {"/api/tasks/42"})
    String path,

    @Schema(description = "Horodatage ISO-8601", examples = {"2026-03-09T14:32:00"},
            type = "string", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp
) {
    public static ErrorDto of(int status, String errorCode, String message, String path) {
        return new ErrorDto(status, errorCode, message, path, LocalDateTime.now());
    }
}
```

> La structure complète (`TaskException`, `@RestControllerAdvice`, tableau des codes) est détaillée en [section 10](#10-gestion-des-erreurs).

### 🔴 Propriétés `@Schema` avancées

| Propriété | Usage | Exemple |
|-----------|-------|---------|
| `description` | Texte explicatif | `"Titre de la tâche"` |
| `examples` | Valeur(s) d'exemple | `{"Ma tâche", "Autre tâche"}` |
| `requiredMode` | `REQUIRED` / `NOT_REQUIRED` | `REQUIRED` |
| `nullable` | Peut être null | `true` |
| `minLength` / `maxLength` | Contraintes string | `1` / `100` |
| `minimum` / `maximum` | Contraintes numériques | `0` / `999` |
| `pattern` | Regex de validation | `"^[A-Z]{2}\\d{3}$"` |
| `accessMode` | `READ_ONLY` / `WRITE_ONLY` / `READ_WRITE` | `READ_ONLY` |
| `hidden` | Masquer du schéma | `true` |
| `defaultValue` | Valeur par défaut | `"false"` |
| `type` / `format` | Type OpenAPI explicite | `type = "string", format = "date-time"` |

---

## 8. Headers HTTP

### 🟢 `Accept-Language` — au-delà de la documentation

`Accept-Language` est un header HTTP standard que Swagger UI documente **et** que Spring utilise réellement pour adapter les messages de réponse à la langue du client.

Dans ce projet, il est branché sur un vrai mécanisme i18n :

```
Requête  →  Accept-Language: es
                │
                ▼
        AcceptHeaderLocaleResolver   →  Locale = "es"
                │
                ▼ (via LocaleContextHolder)
        TaskControllerAdvice         →  MessageSource
                │
                ▼
        messages_es.properties       →  "El título de la tarea no puede estar vacío"
```

### 🟡 Configuration i18n — `I18nConfig`

```java
@Configuration
public class I18nConfig {

    // Résout la locale depuis le header Accept-Language
    // Repli sur "fr" si la langue n'est pas supportée
    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.FRENCH, Locale.ENGLISH, Locale.forLanguageTag("es")));
        resolver.setDefaultLocale(Locale.FRENCH);
        return resolver;
    }

    // Charge les fichiers messages_fr/en/es.properties
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
```

### 🟡 Fichiers de messages externalisés

Toutes les chaînes d'erreur sont externalisées dans des fichiers `.properties` par langue :

```
src/main/resources/
├── messages.properties       ← repli (français)
├── messages_fr.properties
├── messages_en.properties
└── messages_es.properties
```

Exemple de contenu :

```properties
# messages_fr.properties
error.task.not_found=Tâche introuvable avec l''identifiant : {0}
error.task.body_required=Le corps de la requête est obligatoire
error.task.title_blank=Le titre de la tâche ne peut pas être vide
error.task.title_too_long=Le titre ne peut pas dépasser 100 caractères
error.task.description_too_long=La description ne peut pas dépasser 500 caractères
error.request.body_unreadable=Le corps de la requête est absent ou malformé
error.request.missing_param=Paramètre obligatoire manquant : {0}
error.request.invalid_param_type=Le paramètre ''{0}'' doit être de type {1}
error.security.access_denied=Vous n''avez pas les droits nécessaires pour effectuer cette action
error.internal=Une erreur interne est survenue. Veuillez réessayer ultérieurement.
```

```properties
# messages_en.properties
error.task.not_found=Task not found with identifier: {0}
error.task.body_required=Request body is required
error.task.title_blank=Task title cannot be blank
error.task.title_too_long=Title cannot exceed 100 characters
error.task.description_too_long=Description cannot exceed 500 characters
error.request.body_unreadable=Request body is missing or malformed
error.request.missing_param=Required parameter is missing: {0}
error.request.invalid_param_type=Parameter ''{0}'' must be of type {1}
error.security.access_denied=You do not have the required permissions to perform this action
error.internal=An internal error occurred. Please try again later.
```

```properties
# messages_es.properties
error.task.not_found=Tarea no encontrada con el identificador: {0}
error.task.body_required=El cuerpo de la solicitud es obligatorio
error.task.title_blank=El título de la tarea no puede estar vacío
error.task.title_too_long=El título no puede superar los 100 caracteres
error.task.description_too_long=La descripción no puede superar los 500 caracteres
error.request.body_unreadable=El cuerpo de la solicitud está ausente o tiene un formato incorrecto
error.request.missing_param=Falta el parámetro obligatorio: {0}
error.request.invalid_param_type=El parámetro ''{0}'' debe ser de tipo {1}
error.security.access_denied=No tiene los permisos necesarios para realizar esta acción
error.internal=Se ha producido un error interno. Por favor, inténtelo de nuevo más tarde.
```

### 🟡 Résolution dans `TaskControllerAdvice`

Le `ControllerAdvice` injecte `MessageSource` et résout les messages au moment de la requête, quand la locale est connue :

```java
@RestControllerAdvice
public class TaskControllerAdvice {

    private final MessageSource messageSource;

    // Méthode utilitaire — locale lue depuis LocaleContextHolder (alimenté par AcceptHeaderLocaleResolver)
    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    @ExceptionHandler(TaskException.class)
    public ResponseEntity<ErrorDto> handleTaskException(TaskException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorDto.of(
                    ex.getStatus().value(),
                    ex.getErrorCode(),
                    msg(ex.getMessageKey(), ex.getMessageArgs()),  // ← résolution i18n ici
                    request.getRequestURI()
                ));
    }
}
```

### 🟡 `TaskException` porte une clé, pas un message

Les exceptions métier ne contiennent plus de texte hardcodé mais une **clé i18n** et des **arguments** :

```java
// Dans TaskService
throw TaskException.notFound(id);
// → porte la clé "error.task.not_found" et l'argument id
// → le message réel sera résolu selon la locale au moment du rendu

throw TaskException.badRequest("error.task.title_blank");
// → porte la clé "error.task.title_blank", aucun argument
```

### 🟡 Header par endpoint avec `@Parameter`

Pour documenter `Accept-Language` sur un seul endpoint :

```java
@Operation(summary = "Récupérer une tâche")
@Parameter(
    name = "Accept-Language",
    description = "Langue préférée pour les messages de réponse (fr, en, es)",
    in = ParameterIn.HEADER,
    required = false,
    example = "fr",
    schema = @Schema(type = "string", defaultValue = "fr")
)
@GetMapping("/{id}")
ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id);
```

### 🔴 Headers globaux avec `OperationCustomizer`

Pour ajouter automatiquement `Accept-Language` sur **tous les endpoints** :

```java
@Configuration
public class GlobalHeadersConfig {

    @Bean
    public OperationCustomizer globalHeaders() {
        return (Operation operation, HandlerMethod ignored) -> {
            operation.addParametersItem(
                new Parameter()
                    .in("header")
                    .name("Accept-Language")
                    .description("Langue préférée (fr, en, es) — pilote la langue des messages d'erreur")
                    .required(false)
                    .example("fr")
                    .schema(new StringSchema()._default("fr"))
            );
            return operation;
        };
    }
}
```

> 🔴 **Ce header a un effet réel** : Spring lit `Accept-Language`, résout la locale via `AcceptHeaderLocaleResolver`, et tous les messages d'erreur sont retournés dans la langue demandée.

---

## 9. Authentification

### 🟢 Le bouton "Authorize"

Swagger UI affiche un bouton **Authorize** en haut à droite dès que vous avez déclaré des schémas de sécurité dans votre `OpenApiConfig`. Ce bouton permet de saisir vos credentials une fois pour toutes et de les envoyer automatiquement dans chaque requête de test.

> Dans ce projet, deux modes coexistent : **Basic Auth** (username/password) et **Bearer token** (token statique en mémoire).

### 🟡 Déclarer plusieurs schémas dans `OpenApiConfig`

Les deux schémas sont enregistrés dans les `Components`, puis tous deux sont activés globalement via `addSecurityItem` :

```java
@Bean
public OpenAPI taskManagerOpenAPI() {
    return new OpenAPI()
            .info(apiInfo())
            // Active les deux schémas sur toute l'API
            .addSecurityItem(new SecurityRequirement()
                    .addList("basicAuth")
                    .addList("BearerAuth"))
            .components(new Components()
                    // Basic Auth — Authorization: Basic dXNlcjpwYXNz
                    .addSecuritySchemes("basicAuth",
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("basic")
                                    .description("Identifiants basiques (username / password)"))
                    // Bearer token — Authorization: Bearer <token>
                    .addSecuritySchemes("BearerAuth",
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("Token Bearer — format : `Bearer <token>`"))
            );
}
```

> 💡 Bonne pratique : déclarer les noms des schémas en **constantes** pour éviter les fautes de frappe entre `OpenApiConfig` et les `@SecurityRequirement` :

```java
private static final String BASIC_AUTH  = "basicAuth";
private static final String BEARER_AUTH = "BearerAuth";
```

### 🟡 Référencer les deux schémas sur un endpoint d'écriture

Sur les endpoints protégés (POST, PUT, DELETE), on déclare les deux schémas dans `security = {}` pour que Swagger UI sache que l'un **ou** l'autre suffit :

```java
@Operation(
    summary = "Créer une tâche",
    security = {
        @SecurityRequirement(name = "basicAuth"),
        @SecurityRequirement(name = "BearerAuth")
    }
)
@PostMapping
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
ResponseEntity<TaskResponse> createTask(...);
```

> ⚠️ Lorsque `security = {}` est défini sur une `@Operation`, il **remplace** le `addSecurityItem` global pour cet endpoint. Il faut donc bien lister **tous** les schémas acceptés.

### 🟡 Utilisation dans Swagger UI

**Avec Basic Auth :**
1. Cliquer sur **Authorize**
2. Renseigner `username` et `password` dans la section `basicAuth`
3. Cliquer **Authorize** → le header `Authorization: Basic ...` est ajouté automatiquement

**Avec Bearer token :**
1. Cliquer sur **Authorize**
2. Coller le token dans la section `BearerAuth` (sans le préfixe `Bearer`)
3. Cliquer **Authorize** → le header `Authorization: Bearer <token>` est ajouté automatiquement

### 🟡 Filtre Bearer token custom

Quand on n'utilise pas JWT mais des tokens statiques ou opaques, on implémente un `OncePerRequestFilter` qui lit le header `Authorization: Bearer ...`, résout l'utilisateur et alimente le `SecurityContext` :

```java
@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenAuthenticationService tokenAuthenticationService;

    public BearerTokenAuthenticationFilter(TokenAuthenticationService tokenAuthenticationService) {
        this.tokenAuthenticationService = tokenAuthenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            UserDetails userDetails = tokenAuthenticationService.findByToken(token);

            if (userDetails != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities())
                );
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

Le service de résolution des tokens est simple — une `Map` token → `UserDetails` :

```java
@Service
public class TokenAuthenticationService {

    private static final Map<String, UserDetails> TOKENS = Map.of(
        "f4b8c9c7c3e14f0e8a3d9c71d2e7c6c5",
            new User("user", "", List.of(new SimpleGrantedAuthority("ROLE_USER"))),
        "a8c1e0c7-1e9f-4d9b-b4a1-7c2e91c3a0e2",
            new User("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
    );

    public UserDetails findByToken(String token) {
        return TOKENS.get(token);
    }
}
```

### 🔴 Enregistrement du filtre dans `SecurityConfig`

Le filtre est branché **avant** le filtre d'authentification standard de Spring Security :

```java
.addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

> ⚠️ Piège classique avec Swagger UI : un `@Component` `OncePerRequestFilter` est **automatiquement enregistré** par Spring Boot sur la servlet **en plus** du filtre de sécurité, ce qui crée un double passage et provoque des 401 inattendus depuis le navigateur.
>
> La solution : déclarer un `FilterRegistrationBean` qui désactive cet enregistrement auto :

```java
@Bean
public FilterRegistrationBean<BearerTokenAuthenticationFilter> bearerTokenFilterRegistration(
        BearerTokenAuthenticationFilter filter) {
    FilterRegistrationBean<BearerTokenAuthenticationFilter> registration =
            new FilterRegistrationBean<>(filter);
    registration.setEnabled(false); // ← désactive l'enregistrement sur la servlet
    return registration;
}
```

> Sans ce bean, curl fonctionne (une seule requête, un seul passage du filtre) mais Swagger UI échoue avec un 401 (le filtre passe deux fois et efface le `SecurityContext` au deuxième passage).

### 🔴 API Key (header ou query)

```java
.addSecuritySchemes("apiKey",
    new SecurityScheme()
        .type(SecurityScheme.Type.APIKEY)
        .in(SecurityScheme.In.HEADER)
        .name("X-API-Key")
        .description("Clé API fournie lors de l'inscription")
)
```

### 🔴 OAuth2 (pour information)

```java
.addSecuritySchemes("oauth2",
    new SecurityScheme()
        .type(SecurityScheme.Type.OAUTH2)
        .flows(new OAuthFlows()
            .authorizationCode(new OAuthFlow()
                .authorizationUrl("https://auth.exemple.com/oauth/authorize")
                .tokenUrl("https://auth.exemple.com/oauth/token")
                .scopes(new Scopes()
                    .addString("read:tasks", "Lire les tâches")
                    .addString("write:tasks", "Créer/modifier les tâches")
                )
            )
        )
)
```

---


## 10. Gestion des erreurs

### 🟢 Le problème sans gestion centralisée

Sans mécanisme dédié, chaque controller gère ses erreurs différemment : certains retournent un `String`, d'autres un objet vide, d'autres encore laissent Spring retourner sa page HTML d'erreur par défaut. Le consommateur de l'API ne sait pas à quoi s'attendre.

> La solution : **un format d'erreur unique**, **une exception métier**, **un gestionnaire centralisé**.

### 🟢 L'exception métier — `TaskException`

Une exception `RuntimeException` qui porte son propre code HTTP et un code d'erreur lisible :

```java
public class TaskException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String messageKey;
    private final Object[] messageArgs;

    public TaskException(HttpStatus status, String errorCode, String messageKey, Object... messageArgs) {
        super(messageKey);   // message brut = la clé (utile pour les logs)
        this.status      = status;
        this.errorCode   = errorCode;
        this.messageKey  = messageKey;
        this.messageArgs = messageArgs;
    }

    // Fabriques statiques — le site d'appel est expressif et sans duplication
    public static TaskException notFound(Long id) {
        return new TaskException(NOT_FOUND, "TASK_NOT_FOUND", "error.task.not_found", id);
    }

    public static TaskException badRequest(String messageKey, Object... args) {
        return new TaskException(BAD_REQUEST, "TASK_BAD_REQUEST", messageKey, args);
    }

    public static TaskException conflict(String messageKey, Object... args) {
        return new TaskException(CONFLICT, "TASK_CONFLICT", messageKey, args);
    }

    public HttpStatus getStatus()    { return status; }
    public String getErrorCode()     { return errorCode; }
    public String getMessageKey()    { return messageKey; }
    public Object[] getMessageArgs() { return messageArgs; }
}
```

**Avantage des fabriques statiques** : le site d'appel est expressif et sans duplication :

```java
throw TaskException.notFound(id);                         // → 404 TASK_NOT_FOUND
throw TaskException.badRequest("error.task.title_blank"); // → 400 TASK_BAD_REQUEST
```

### 🟢 Le DTO d'erreur — `ErrorDto`

**Un seul format** pour toutes les erreurs de l'API, documenté avec `@Schema` :

```java
@Schema(name = "ErrorDto", description = "Corps de réponse standard pour toutes les erreurs")
public record ErrorDto(
    int status,       // 404
    String errorCode, // "TASK_NOT_FOUND"
    String message,   // "Tâche introuvable avec l'identifiant : 42"
    String path,      // "/api/tasks/42"
    LocalDateTime timestamp
) {}
```

Exemple de réponse JSON que le client recevra :

```json
{
  "status": 404,
  "errorCode": "TASK_NOT_FOUND",
  "message": "Tâche introuvable avec l'identifiant : 42",
  "path": "/api/tasks/42",
  "timestamp": "2026-03-09T14:32:00"
}
```

### 🟡 Le gestionnaire centralisé — `@RestControllerAdvice`

`@RestControllerAdvice` intercepte toutes les exceptions levées par n'importe quel controller et les transforme en `ErrorDto`. **Un seul endroit** pour toute la gestion d'erreur :

```java
@RestControllerAdvice
public class TaskControllerAdvice {

    private final MessageSource messageSource;

    public TaskControllerAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // Résolution i18n — locale lue depuis AcceptHeaderLocaleResolver
    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    // 1. Erreurs métier explicites
    @ExceptionHandler(TaskException.class)
    public ResponseEntity<ErrorDto> handleTaskException(TaskException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorDto.of(ex.getStatus().value(), ex.getErrorCode(),
                        msg(ex.getMessageKey(), ex.getMessageArgs()),  // ← résolution i18n
                        request.getRequestURI()));
    }

    // 2. Body JSON absent ou malformé → 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleNotReadable(HttpMessageNotReadableException ignored,
                                                       HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorDto.of(400, "INVALID_REQUEST_BODY",
                        msg("error.request.body_unreadable"),
                        request.getRequestURI()));
    }

    // 3. Paramètre obligatoire manquant → 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDto> handleMissingParam(MissingServletRequestParameterException ex,
                                                        HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorDto.of(400, "MISSING_PARAMETER",
                        msg("error.request.missing_param", ex.getParameterName()),
                        request.getRequestURI()));
    }

    // 4. Type de paramètre incorrect → 400 (ex: /api/tasks/abc au lieu de /api/tasks/42)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                        HttpServletRequest request) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "?";
        return ResponseEntity.badRequest()
                .body(ErrorDto.of(400, "INVALID_PARAMETER_TYPE",
                        msg("error.request.invalid_param_type", ex.getName(), expected),
                        request.getRequestURI()));
    }

    // 5. Accès refusé — @PreAuthorize échoue → 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException ignored,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorDto.of(403, "ACCESS_DENIED",
                        msg("error.security.access_denied"),
                        request.getRequestURI()));
    }

    // 6. Fallback — tout ce qui n'est pas prévu → 500 (message interne non exposé)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGeneric(Exception ignored, HttpServletRequest request) {
        return ResponseEntity.internalServerError()
                .body(ErrorDto.of(500, "INTERNAL_ERROR",
                        msg("error.internal"),
                        request.getRequestURI()));
    }
}
```

### 🟡 Où vit la validation ?

La règle est simple : **la validation est une responsabilité du service**, pas du controller.

```
Requête HTTP  →  TaskController  →  TaskService.validate()  →  throw TaskException
                  (délègue)           (valide et lève)
                      │
                      ▼
               TaskControllerAdvice   →  ErrorDto JSON
                  (intercepte)
```

```java
// TaskController — aucune validation, délégation totale
@Override
public ResponseEntity<TaskResponse> createTask(TaskRequest request) {
    TaskResponse created = taskService.create(request);   // la validation est dans le service
    return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
}

// TaskService — toute la validation
public TaskResponse create(TaskRequest request) {
    validate(request);   // ← validation ici
    // persistence...
}

private void validate(TaskRequest request) {
    if (request.title() == null || request.title().isBlank())
        throw TaskException.badRequest("Le titre ne peut pas être vide");
    if (request.title().strip().length() > 100)
        throw TaskException.badRequest("Le titre ne peut pas dépasser 100 caractères");
    if (request.description() != null && request.description().length() > 500)
        throw TaskException.badRequest("La description ne peut pas dépasser 500 caractères");
}
```

### 🟡 Documenter les erreurs dans OpenAPI

Chaque endpoint de `TaskApi` documente ses réponses d'erreur avec le schéma `ErrorDto` :

```java
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tâche trouvée",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TaskResponse.class))),
    @ApiResponse(responseCode = "404", description = "Tâche introuvable",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class))),
    @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDto.class)))
    // 500 est ajouté automatiquement par OpenApiCustomizer — pas besoin de le répéter
})
@GetMapping("/{id}")
ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id);
```

### 🔴 Tableau des codes d'erreur métier

| `errorCode` | `status` | Situation |
|-------------|----------|-----------|
| `TASK_NOT_FOUND` | 404 | Tâche inexistante avec cet ID |
| `TASK_BAD_REQUEST` | 400 | Validation échouée (titre vide, trop long...) |
| `TASK_CONFLICT` | 409 | Conflit métier (ex: titre déjà existant) |
| `INVALID_REQUEST_BODY` | 400 | JSON malformé ou body absent |
| `MISSING_PARAMETER` | 400 | Paramètre obligatoire absent |
| `INVALID_PARAMETER_TYPE` | 400 | Mauvais type (ex: `abc` pour un `Long`) |
| `INTERNAL_ERROR` | 500 | Erreur non prévue |

### 🔴 Pourquoi ne pas exposer le message interne sur les 500 ?

```java
// ❌ Dangereux — expose la stacktrace ou des détails internes
return ErrorDto.of(500, "INTERNAL_ERROR", ex.getMessage(), path);

// ✅ Correct — message générique, erreur loguée côté serveur
log.error("Erreur interne sur {}", path, ex);
return ErrorDto.of(500, "INTERNAL_ERROR",
        "Une erreur interne est survenue. Veuillez réessayer ultérieurement.", path);
```

Un message d'erreur interne peut révéler la structure de la base de données, un nom de classe, une requête SQL... C'est un vecteur d'attaque courant.

---

## 11. Bonnes pratiques & erreurs fréquentes

### 🟢 Les 5 règles d'or

| # | Règle | Pourquoi |
|---|-------|----------|
| 1 | **Annotations OpenAPI dans l'interface, pas l'implémentation** | Séparation des responsabilités |
| 2 | **Validation dans le service, pas dans le controller** | Testabilité, réutilisabilité |
| 3 | **Documenter aussi les codes d'erreur (4xx, 5xx) avec `ErrorDto`** | Le contrat doit être complet |
| 4 | **Utiliser des exemples concrets** | Plus utile que "string" |
| 5 | **Désactiver Swagger UI en production** | Sécurité |

### 🟡 Erreurs fréquentes

#### ❌ Erreur 1 — Validation dans le controller

```java
// ❌ MAUVAIS — le controller valide
@Override
public ResponseEntity<TaskResponse> createTask(TaskRequest request) {
    if (request.title() == null || request.title().isBlank()) {
        throw TaskException.badRequest("Le titre ne peut pas être vide");
    }
    // ...
}
```

```java
// ✅ CORRECT — le controller délègue, le service valide
@Override
public ResponseEntity<TaskResponse> createTask(TaskRequest request) {
    TaskResponse created = taskService.create(request);   // la validation est dans le service
    return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
}
```

#### ❌ Erreur 2 — Annotations OpenAPI sur l'implémentation

```java
// ❌ MAUVAIS — annotations OpenAPI sur le controller
@RestController
@Tag(name = "Tâches")  // ← ici c'est faux
public class TaskController implements TaskApi {

    @Operation(summary = "...")  // ← ici aussi
    @Override
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(Boolean done) { ... }
}
```

```java
// ✅ CORRECT — annotations OpenAPI sur l'interface, mapping HTTP sur le controller
@Tag(name = "Tâches")
public interface TaskApi {

    @Operation(summary = "...")
    ResponseEntity<List<TaskResponse>> getAllTasks(Boolean done);
}

@RestController
@RequestMapping("/api/tasks")
public class TaskController implements TaskApi {

    @Override
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(Boolean done) { ... }
}
```

#### ❌ Erreur 3 — Confondre les deux `@RequestBody`

```java
// ❌ Mauvaise importation
import org.springframework.web.bind.annotation.RequestBody;

@RequestBody  // ← c'est Spring MVC, pas OpenAPI !
TaskRequest request
```

```java
// ✅ Correct
import io.swagger.v3.oas.annotations.parameters.RequestBody;  // OpenAPI

@RequestBody(description = "...", content = @Content(...))    // ← OpenAPI (doc)
@org.springframework.web.bind.annotation.RequestBody          // ← Spring MVC (binding)
TaskRequest request
```

#### ❌ Erreur 4 — Ne documenter que les succès

```java
// ❌ Incomplet
@ApiResponse(responseCode = "200", description = "OK")

// ✅ Complet
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tâche trouvée",
                content = @Content(schema = @Schema(implementation = TaskResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tâche introuvable",
                content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
                content = @Content(schema = @Schema(implementation = ErrorDto.class)))
        // 500 est ajouté automatiquement par OpenApiCustomizer — pas besoin de le répéter
})
```

#### ❌ Erreur 5 — Exposer les détails d'une erreur 500

```java
// ❌ Dangereux
body(ErrorDto.of(500, "INTERNAL_ERROR", ex.getMessage(), path));

// ✅ Message générique côté client, log côté serveur
        log.error("Erreur interne sur {}", path, ex);
body(ErrorDto.of(500, "INTERNAL_ERROR", "Une erreur interne est survenue.", path));
```

### 🔴 Checklist avant livraison

- [ ] Tous les endpoints ont un `@Operation` avec `summary` et `description`
- [ ] Tous les codes de réponse sont documentés (`2xx`, `4xx`) — le `500` est global via `OpenApiCustomizer`
- [ ] Les réponses d'erreur utilisent `@Schema(implementation = ErrorDto.class)` (pas `hidden = true`)
- [ ] Chaque paramètre a un `description` et un `example`
- [ ] Les champs des DTO ont tous un `@Schema` avec `description` et `example`
- [ ] Les endpoints sécurisés ont `@SecurityRequirement`
- [ ] Swagger UI est désactivé en production
- [ ] La validation est dans le service, pas dans le controller
- [ ] Les messages des erreurs `500` n'exposent pas d'informations internes
- [ ] Aucune annotation OpenAPI dans le service ou les entités

### 🔴 OpenAPI ne remplace pas le design

> "Documenter une mauvaise API ne la rend pas bonne."

OpenAPI documente **ce que l'API fait**, il ne décide pas **ce qu'elle devrait faire**. Avant d'annoter, assurez-vous que :

- Les noms de ressources sont en **noms, pas en verbes** (`/tasks` et non `/getTasks`)
- Les codes HTTP sont **sémantiquement corrects** (`201` pour la création, `204` pour la suppression sans body)
- La structure JSON est **cohérente** entre les endpoints
- La pagination est **prévue** pour les listes

---

## 12. Le fichier OpenAPI — JSON, YAML & génération de code

### 🟢 Le fichier produit par springdoc

À chaque démarrage, springdoc inspecte vos controllers et génère automatiquement un fichier de description de l'API. Ce fichier est la **source de vérité** du contrat.

Deux URLs exposent ce fichier :

| URL | Format | Usage |
|-----|--------|-------|
| `/v3/api-docs` | JSON | Consommé par Swagger UI, outils CI/CD |
| `/v3/api-docs.yaml` | YAML | Plus lisible, idéal pour la génération de code |

```bash
# Récupérer le JSON
curl http://localhost:8080/v3/api-docs | jq .

# Récupérer le YAML
curl http://localhost:8080/v3/api-docs.yaml
```

### 🟢 Structure du fichier JSON

Le fichier suit la **spécification OpenAPI 3.0**. Voici sa structure de haut niveau :

```json
{
  "openapi": "3.0.1",
  "info": {
    "title": "Task Manager API",
    "version": "1.0.0",
    "description": "API REST de gestion de tâches."
  },
  "paths": {
    "/api/tasks": {
      "get": {
        "tags": ["Tâches"],
        "summary": "Lister toutes les tâches",
        "operationId": "getAllTasks",
        "parameters": [ ... ],
        "responses": {
          "200": { ... },
          "500": { ... }
        }
      },
      "post": { ... }
    },
    "/api/tasks/{id}": { ... }
  },
  "components": {
    "schemas": {
      "TaskRequest":  { ... },
      "TaskResponse": { ... },
      "ErrorDto":     { ... }
    },
    "securitySchemes": {
      "basicAuth":   { "type": "http", "scheme": "basic" },
      "BearerAuth":  { "type": "http", "scheme": "bearer" }
    }
  }
}
```

Les trois grandes sections :
- **`info`** → ce que vous avez configuré dans le bean `OpenAPI`
- **`paths`** → un objet par endpoint, généré depuis `TaskApi`
- **`components`** → les schémas réutilisables (`@Schema` sur les DTO) et les schémas de sécurité

### 🟢 Le même fichier en YAML

Le YAML est strictement équivalent au JSON, mais plus lisible pour un humain :

```yaml
openapi: 3.0.1
info:
  title: Task Manager API
  version: 1.0.0

paths:
  /api/tasks:
    get:
      tags:
        - Tâches
      summary: Lister toutes les tâches
      parameters:
        - name: done
          in: query
          required: false
          schema:
            type: boolean
      responses:
        "200":
          description: Liste récupérée avec succès
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/TaskResponse"
        "500":
          description: Erreur interne du serveur
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorDto"

components:
  schemas:
    TaskRequest:
      type: object
      properties:
        title:
          type: string
          description: Titre de la tâche
          example: Préparer la présentation OpenAPI
          minLength: 1
          maxLength: 100
        description:
          type: string
          nullable: true
        done:
          type: boolean
          example: false
      required:
        - title
        - done
```

> Le mécanisme `$ref: "#/components/schemas/TaskResponse"` est la clé : plutôt que de répéter la structure partout, OpenAPI **référence** le schéma déclaré dans `components`. C'est ce que produit `@Schema(implementation = TaskResponse.class)`.

### 🟡 Exporter le fichier au build Maven

En production, on ne veut pas forcément démarrer l'application pour récupérer le fichier. Le plugin Maven `springdoc-openapi-maven-plugin` génère le fichier **statiquement au build** :

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>2.0.0</version>
    <executions>
        <execution>
            <id>generate-openapi</id>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <apiDocsUrl>http://localhost:8080/v3/api-docs.yaml</apiDocsUrl>
        <outputFileName>openapi.yaml</outputFileName>
        <outputDir>${project.build.directory}</outputDir>
    </configuration>
</plugin>
```

```bash
mvn verify          # démarre l'app, génère target/openapi.yaml, arrête l'app
```

Le fichier `target/openapi.yaml` peut ensuite être **versionné**, **publié** ou **utilisé pour générer du code**.

### 🟡 Code First vs Design First

| Approche | Description | Quand l'utiliser |
|----------|-------------|-----------------|
| **Code First** *(ce projet)* | On écrit le code Spring, springdoc génère le YAML | Équipe back autonome, API interne |
| **Design First** | On écrit le YAML d'abord, on génère le code depuis | API publique, contrat partagé avant l'implémentation |

### 🔴 Générer du code à partir du fichier YAML

C'est la direction **inverse** : partir du fichier YAML pour générer automatiquement du code (interfaces, modèles, clients). L'outil standard est **OpenAPI Generator**.

#### Installation

```bash
# Via Homebrew
brew install openapi-generator

# Via npm
npm install -g @openapitools/openapi-generator-cli

# Via Maven (plugin intégré au build)
# → voir ci-dessous
```

#### Générer un client TypeScript (pour une appli front)

```bash
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs.yaml \
  -g typescript-axios \
  -o ./client-ts
```

Résultat : un SDK TypeScript prêt à l'emploi, avec les interfaces `TaskRequest`, `TaskResponse`, `ErrorDto` et les méthodes `getAllTasks()`, `createTask()`, etc.

#### Générer les interfaces Java côté serveur (Design First)

```bash
openapi-generator-cli generate \
  -i openapi.yaml \
  -g spring \
  -o ./generated \
  --additional-properties=interfaceOnly=true,useSpringBoot3=true
```

Option `interfaceOnly=true` → génère uniquement les interfaces (le contrat), **pas l'implémentation**.  
Vous n'avez plus qu'à écrire `TaskController implements TaskApi` — exactement le pattern de ce projet.

#### Via le plugin Maven (intégré au build)

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.4.0</version>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <configOptions>
                    <interfaceOnly>true</interfaceOnly>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useTags>true</useTags>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### Générateurs disponibles (non exhaustif)

| Générateur | Ce qu'il produit |
|------------|-----------------|
| `spring` | Interfaces + modèles Java (serveur) |
| `typescript-axios` | Client TypeScript avec Axios |
| `typescript-fetch` | Client TypeScript avec fetch natif |
| `python` | Client Python |
| `go` | Client Go |
| `kotlin` | Client ou serveur Kotlin |
| `html2` | Documentation HTML statique |
| `markdown` | Documentation Markdown |

```bash
# Lister tous les générateurs disponibles
openapi-generator-cli list
```

### 🔴 Workflow Design First complet

```
1. Écrire / éditer  →  openapi.yaml  (Swagger Editor : editor.swagger.io)
        │
        ▼
2. Valider          →  openapi-generator-cli validate -i openapi.yaml
        │
        ▼
3. Générer          →  Interfaces Java (back) + SDK TypeScript (front)
        │
        ▼
4. Implémenter      →  TaskController implements TaskApi  (code généré = contrat)
        │
        ▼
5. Vérifier         →  springdoc génère le JSON au runtime
                        → doit correspondre au YAML source
```

> ⚠️ En Design First, le fichier YAML est la **source de vérité**. Le code généré ne doit **jamais être modifié à la main** — il sera écrasé à la prochaine génération. Seule l'implémentation (`TaskController`) est écrite manuellement.

---

## 13. Conclusion

### 🟢 Résumé en 3 phrases

**OpenAPI** est le **standard** qui décrit votre API REST de façon lisible par humains et machines.  
**Swagger UI** est l'**outil** qui affiche cette description sous forme d'interface web interactive.  
**springdoc-openapi** est l'**intégration** qui génère tout cela automatiquement depuis votre code Spring Boot.

### 🟡 Ce que vous avez appris

| Section | Concept clé |
|---------|-------------|
| Concepts | OpenAPI = contrat partagé, Swagger UI = outil de lecture/test |
| Architecture | Doc dans l'adaptateur REST, validation dans le service, jamais dans le domaine |
| Installation | 1 dépendance, 2 URLs (`/swagger-ui.html`, `/v3/api-docs`) |
| Configuration | Bean `OpenAPI` pour titre/version/sécurité + `OpenApiCustomizer` pour le 500 global |
| Interface | Contrat dans l'interface, controller = délégation, service = logique |
| Annotations | `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, `@SecurityRequirement` |
| DTO | `@Schema` sur record/classe — `TaskRequest`, `TaskResponse`, `ErrorDto` |
| Headers & i18n | `Accept-Language` documenté dans Swagger **et** branché sur `AcceptHeaderLocaleResolver` → messages traduits (fr/en/es) |
| Auth | `basicAuth` + `BearerAuth` déclarés dans `OpenApiConfig`, bouton Authorize dans Swagger UI |
| Erreurs | `TaskException` (clé i18n) + `ErrorDto` + `@RestControllerAdvice` + résolution `MessageSource` |
| Fichier OpenAPI | `/v3/api-docs` (JSON) · `/v3/api-docs.yaml` (YAML) · génération de code avec OpenAPI Generator |

### 🔴 Aller plus loin

| Sujet | Ressource |
|-------|-----------|
| Spécification OpenAPI 3.1 | https://spec.openapis.org/oas/v3.1.0 |
| springdoc-openapi docs | https://springdoc.org |
| Génération SDK client | OpenAPI Generator (https://openapi-generator.tech) |
| Design First | Swagger Editor (https://editor.swagger.io) |
| Tests de contrat | Pact (https://pact.io) |

### 🔴 OpenAPI comme contrat vivant

```
        Équipe Back-end           Équipe Front-end
              │                         │
              ▼                         ▼
      Code Spring Boot          SDK TypeScript généré
              │                         │
              └──────────► JSON OpenAPI ◄──────────┘
                              (contrat)
                                 │
                                 ▼
                          Tests de contrat
                          (Pact, Schemathesis)
```

> Le vrai pouvoir d'OpenAPI n'est pas la page de documentation jolie — c'est la **génération automatique** de clients, de tests et de mocks à partir d'un seul source de vérité.

---

*Support de cours rédigé pour le projet `01-openapi` — Spring Boot 4.0.3 · Java 25 · springdoc 3.0.2*
