package com.Group.SecServSet.repo;

import com.Group.SecServSet.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepo extends JpaRepository<Task,Long> { }
