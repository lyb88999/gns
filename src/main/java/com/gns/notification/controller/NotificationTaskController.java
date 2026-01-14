package com.gns.notification.controller;

import com.gns.notification.dto.NotificationTaskRequest;
import com.gns.notification.dto.NotificationTaskResponse;
import com.gns.notification.dto.PageResult;
import com.gns.notification.service.NotificationTaskService;
import com.gns.notification.service.scheduler.TaskExecutionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class NotificationTaskController {

    private final NotificationTaskService taskService;
    private final TaskExecutionService taskExecutionService;

    public NotificationTaskController(NotificationTaskService taskService, TaskExecutionService taskExecutionService) {
        this.taskService = taskService;
        this.taskExecutionService = taskExecutionService;
    }

    @PostMapping
    public ResponseEntity<NotificationTaskResponse> createTask(@Valid @RequestBody NotificationTaskRequest request) {
        return ResponseEntity.ok(taskService.createTask(request));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<NotificationTaskResponse> updateTask(
        @PathVariable String taskId,
        @Valid @RequestBody NotificationTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<NotificationTaskResponse> getTask(@PathVariable String taskId) {
        return ResponseEntity.ok(taskService.getTask(taskId));
    }

    @GetMapping
    public ResponseEntity<PageResult<NotificationTaskResponse>> listTasks(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskService.listTasks(PageRequest.of(page, size)));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/execute")
    public ResponseEntity<Void> executeTask(@PathVariable String taskId) {
        taskExecutionService.executeTask(taskId);
        return ResponseEntity.ok().build();
    }
}
