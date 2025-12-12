package com.Group.SecServSet.repo;

import com.Group.SecServSet.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepo extends JpaRepository<Task,Long> {
    List<Task> findByOwner_Username(String username);
}
