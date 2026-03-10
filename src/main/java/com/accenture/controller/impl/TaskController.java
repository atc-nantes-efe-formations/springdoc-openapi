package com.accenture.controller.impl;

import com.accenture.controller.TaskApi;
import com.accenture.dto.TaskRequest;
import com.accenture.dto.TaskResponse;
import com.accenture.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/api/tasks")
public class TaskController implements TaskApi {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @RequestParam(required = false) Boolean done) {
        return ResponseEntity.ok(taskService.findAll(done));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @Override
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @org.springframework.web.bind.annotation.RequestBody TaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
