package com.example.beinterviewprep.repository;

import com.example.beinterviewprep.entity.Task;
import com.example.beinterviewprep.entity.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);
}
