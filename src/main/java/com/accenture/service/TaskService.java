package com.accenture.service;

import com.accenture.dto.TaskRequest;
import com.accenture.dto.TaskResponse;
import com.accenture.exception.TaskException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service métier pour la gestion des tâches.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>Validation des données en entrée</li>
 *   <li>Logique métier (création, lecture, mise à jour, suppression)</li>
 *   <li>Levée des {@link TaskException} en cas d'erreur</li>
 * </ul>
 *
 * <p>Le controller ne fait que déléguer ici — il ne contient aucune logique.</p>
 */
@Service
public class TaskService {

    // Stockage en mémoire pour la démo (pas de base de données)
    private final Map<Long, TaskResponse> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // =========================================================================
    // Lecture
    // =========================================================================

    public List<TaskResponse> findAll(Boolean done) {
        List<TaskResponse> result = new ArrayList<>(store.values());

        if (done != null) {
            result = result.stream()
                    .filter(t -> t.done() == done)
                    .toList();
        }

        return result;
    }

    public TaskResponse findById(Long id) {
        TaskResponse task = store.get(id);
        if (task == null) {
            throw TaskException.notFound(id);
        }
        return task;
    }

    // =========================================================================
    // Écriture
    // =========================================================================

    public TaskResponse create(TaskRequest request) {
        validate(request);

        Long id = idGenerator.getAndIncrement();
        TaskResponse response = new TaskResponse(
                id,
                request.title().strip(),
                request.description(),
                request.done()
        );
        store.put(id, response);
        return response;
    }

    public TaskResponse update(Long id, TaskRequest request) {
        if (!store.containsKey(id)) {
            throw TaskException.notFound(id);
        }
        validate(request);

        TaskResponse updated = new TaskResponse(
                id,
                request.title().strip(),
                request.description(),
                request.done()
        );
        store.put(id, updated);
        return updated;
    }

    public void delete(Long id) {
        if (!store.containsKey(id)) {
            throw TaskException.notFound(id);
        }
        store.remove(id);
    }

    // =========================================================================
    // Validation — centralisée ici, jamais dans le controller
    // =========================================================================

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

