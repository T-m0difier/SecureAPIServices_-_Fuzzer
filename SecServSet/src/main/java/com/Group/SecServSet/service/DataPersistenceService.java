package com.Group.SecServSet.service;

import com.Group.SecServSet.model.*;
import com.Group.SecServSet.repo.TaskRepo;
import com.Group.SecServSet.repo.UserRepo;
import jakarta.annotation.PreDestroy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.*;
import java.util.*;


@Service
public class DataPersistenceService {

    private final UserRepo userRepo;
    private final TaskRepo taskRepo;
    private final PasswordEncoder encoder;

    private final File usersFile = new File("users.txt");
    private final File tasksFile = new File("tasks.txt");

    public DataPersistenceService(UserRepo userRepo, TaskRepo taskRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.taskRepo = taskRepo;
        this.encoder = encoder;
    }

    // Load data when application starts
    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        loadUsers();
        loadTasks();
    }

    private void loadUsers() {
        if (!usersFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(usersFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Format: username|email|age|password|role
                String[] p = line.split("\\|");

                User u = new User();
                u.setUsername(p[0]);
                u.setEmail(p[1]);
                u.setAge(Integer.parseInt(p[2]));
                u.setPassword(p[3]); // already encoded
                u.setRole(Role.valueOf(p[4]));

                userRepo.save(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTasks() {
        if (!tasksFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(tasksFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Format: title|description|status
                String[] p = line.split("\\|");

                Task t = new Task();
                t.setTitle(p[0]);
                t.setDescription(p[1]);
                t.setStatus(Status.valueOf(p[2]));

                taskRepo.save(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save data on shutdown
    @PreDestroy
    public void saveData() {
        saveUsers();
        saveTasks();
    }

    private void saveUsers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(usersFile))) {
            for (User u : userRepo.findAll()) {
                pw.println(
                        u.getUsername() + "|" +
                                u.getEmail() + "|" +
                                (u.getAge() == null ? 0 : u.getAge()) + "|" +
                                u.getPassword() + "|" +
                                u.getRole()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveTasks() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(tasksFile))) {
            for (Task t : taskRepo.findAll()) {
                pw.println(
                        t.getTitle() + "|" +
                                t.getDescription() + "|" +
                                t.getStatus()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
