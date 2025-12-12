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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class DataPersistenceService {

    private final UserRepo userRepo;
    private final TaskRepo taskRepo;
    private final PasswordEncoder encoder;

    private final ObjectMapper objectMapper;

    private final File usersFile = new File("users.json");
    private final File tasksFile = new File("tasks.json");

    public DataPersistenceService(UserRepo userRepo, TaskRepo taskRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.taskRepo = taskRepo;
        this.encoder = encoder;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);


    }

    // Load data when application starts
    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        loadUsers();
        loadTasks();
    }

    //Load Users
    private void loadUsers() {
        if (!usersFile.exists() || usersFile.length() == 0) return;

        try {

            List<User> users = objectMapper.readValue(usersFile, new TypeReference<List<User>>() {});
            userRepo.saveAll(users);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Load Tasks
    private void loadTasks() {
        if (!tasksFile.exists() || tasksFile.length() == 0) return;

        try {
            List<Task> tasks = objectMapper.readValue(tasksFile, new TypeReference<List<Task>>() {});
            taskRepo.saveAll(tasks);
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

    //Save Users
    private void saveUsers() {
        try {
            List<User> users = userRepo.findAll();
            objectMapper.writeValue(usersFile, users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Save Tasks
    private void saveTasks() {
        try {
            List<Task> tasks = taskRepo.findAll();
            objectMapper.writeValue(tasksFile, tasks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


