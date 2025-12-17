package com.Group.SecServSet.control;

import com.Group.SecServSet.model.Task;
import com.Group.SecServSet.repo.TaskRepo;
import com.Group.SecServSet.model.User;
import com.Group.SecServSet.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


@RestController
@RequestMapping("/tasks")
public class TaskControl {

    private final TaskRepo taskRepository;
    private final UserRepo userRepo;

    public TaskControl(TaskRepo taskRepository, UserRepo userRepo) {
        this.taskRepository = taskRepository;
        this.userRepo = userRepo;
    }

    //create task endpoint
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task) {

        // task endpoints validation/ error messages
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task title is required"));
        }

        if (task.getDescription() == null || task.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task description is required"));
        }

        if (task.getStatus() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task status is required"));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        task.setOwner(owner);
        taskRepository.save(task);
        return ResponseEntity.ok(Map.of("message", "Task created", "id", task.getId()));
    }

    //list/get all tasks endpoint (users see their own, admins see everyone's)
    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<Task> tasks = isAdmin
                ? taskRepository.findAll()
                : taskRepository.findByOwner_Username(username);
        if (tasks.isEmpty()) {
            return ResponseEntity.ok("Tasks table is empty");
        }
        return ResponseEntity.ok(tasks);
    }

    // get task according to id endpoint (users their own, admins everyone's)
    @GetMapping("/{id}")
    public ResponseEntity<?> getTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task with ID " + id + " not found"));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !task.getOwner().getUsername().equals(username)) {
            return ResponseEntity.status(403).body(Map.of("error", "You can view only your own tasks"));
        }
        return ResponseEntity.ok(task);
    }

    // update task endpoint (users their own, admins everyone's)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Task updatedTask) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task with ID " + id + " not found"));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                        .getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !task.getOwner().getUsername().equals(username)) {
            return ResponseEntity.status(403).body(Map.of("error", "You can update only your own tasks"));
        }


        task.setTitle(updatedTask.getTitle());
        task.setDescription(updatedTask.getDescription());
        task.setStatus(updatedTask.getStatus());
        taskRepository.save(task);
        return ResponseEntity.ok("Task updated with ID: " + task.getId());
    }

    //delete task endpoint (users their own, admins everyone's)

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {


        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task with ID " + id + " not found"));
        String taskTitle = task.getTitle();

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !task.getOwner().getUsername().equals(username)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "You may delete ONLY your own tasks"));
        }

        taskRepository.delete(task);
        return ResponseEntity.ok(Map.of(
                "message", "Task deleted successfully",
                "deletedTaskId", id,
                "deletedTaskTitle", taskTitle
        ));
    }



}


