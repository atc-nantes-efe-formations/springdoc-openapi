package com.accenture.controller.impl;

import com.accenture.controller.TaskApi;
import com.accenture.dto.TaskRequest;
import com.accenture.dto.TaskResponse;
import com.accenture.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Implémentation du contrat {@link TaskApi}.
 *
 * <p>Ce controller ne fait que déléguer au {@link TaskService}.
 * Aucune logique métier, aucune validation, aucune annotation OpenAPI ici.</p>
 *
 * <p>Responsabilité unique : adapter la réponse du service en {@link ResponseEntity}.</p>
 */
@RestController
public class TaskController implements TaskApi {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public ResponseEntity<List<TaskResponse>> getAllTasks(Boolean done) {
        return ResponseEntity.ok(taskService.findAll(done));
    }

    @Override
    public ResponseEntity<TaskResponse> getTaskById(Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @Override
    public ResponseEntity<TaskResponse> createTask(TaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @Override
    public ResponseEntity<TaskResponse> updateTask(Long id, TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> deleteTask(Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
