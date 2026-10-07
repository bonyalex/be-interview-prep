package com.example.beinterviewprep.service;

import com.example.beinterviewprep.dto.TaskRequest;
import com.example.beinterviewprep.dto.TaskResponse;
import com.example.beinterviewprep.entity.Task;
import com.example.beinterviewprep.entity.TaskStatus;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.repository.TaskRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        apply(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null ? taskRepository.findAll() : taskRepository.findByStatus(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(find(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = find(id);
        apply(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(find(id));
    }

    private Task find(Long id) {
        return taskRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + id + " not found"));
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(request.status() == null ? TaskStatus.TO_DO : request.status());
        task.setDueDate(request.dueDate());
    }
}
