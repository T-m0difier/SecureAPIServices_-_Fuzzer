package com.Group.SecServSet.control;

import com.Group.SecServSet.model.Task;
import com.Group.SecServSet.repo.TaskRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


@RestController
@RequestMapping("/tasks")
public class TaskControl {

    private final TaskRepo taskRepository;

    public TaskControl(TaskRepo taskRepository) {
        this.taskRepository = taskRepository;
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task) {

        // VALIDATION
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task title is required"));
        }

        if (task.getDescription() == null || task.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task description is required"));
        }

        if (task.getStatus() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task status is required"));
        }

        taskRepository.save(task);
        return ResponseEntity.ok(Map.of("message", "Task created", "id", task.getId()));
    }

    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        if (tasks.isEmpty()) {
            return ResponseEntity.ok("Tasks table is empty");
        }
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task with ID " + id + " not found"));
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Task updatedTask) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task with ID " + id + " not found"));

        task.setTitle(updatedTask.getTitle());
        task.setDescription(updatedTask.getDescription());
        task.setStatus(updatedTask.getStatus());

        taskRepository.save(task);
        return ResponseEntity.ok("Task updated with ID: " + task.getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.ok("Task with ID " + id + " does not exist");
        }
        taskRepository.deleteById(id);
        return ResponseEntity.ok("Task deleted with ID: " + id);
    }



}


