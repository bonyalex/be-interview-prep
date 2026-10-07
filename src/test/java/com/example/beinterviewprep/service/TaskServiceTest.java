package com.example.beinterviewprep.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.dto.TaskRequest;
import com.example.beinterviewprep.dto.TaskResponse;
import com.example.beinterviewprep.entity.Task;
import com.example.beinterviewprep.entity.TaskStatus;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createDefaultsStatusToToDoWhenOmitted() {
        TaskRequest request = new TaskRequest("Write report", "Quarterly", null, LocalDate.now().plusDays(1));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(request);

        assertEquals(TaskStatus.TO_DO, response.status());
        assertEquals("Write report", response.title());
    }

    @Test
    void listWithoutStatusReturnsAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(task(1L, TaskStatus.DONE), task(2L, TaskStatus.TO_DO)));

        List<TaskResponse> responses = taskService.list(null);

        assertEquals(2, responses.size());
    }

    @Test
    void listWithStatusFiltersByThatStatus() {
        when(taskRepository.findByStatus(TaskStatus.DONE)).thenReturn(List.of(task(1L, TaskStatus.DONE)));

        List<TaskResponse> responses = taskService.list(TaskStatus.DONE);

        assertEquals(1, responses.size());
        assertEquals(TaskStatus.DONE, responses.get(0).status());
        verify(taskRepository, never()).findAll();
    }

    @Test
    void getUnknownTaskThrowsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.get(99L));
    }

    @Test
    void updateChangesFieldsOfExistingTask() {
        Task existing = task(1L, TaskStatus.TO_DO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);
        TaskRequest request = new TaskRequest("New title", "New description", TaskStatus.IN_PROGRESS, null);

        TaskResponse response = taskService.update(1L, request);

        assertEquals("New title", response.title());
        assertEquals(TaskStatus.IN_PROGRESS, response.status());
    }

    @Test
    void updateUnknownTaskThrowsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());
        TaskRequest request = new TaskRequest("Title", null, TaskStatus.DONE, null);

        assertThrows(ResourceNotFoundException.class, () -> taskService.update(99L, request));
    }

    @Test
    void deleteRemovesExistingTask() {
        Task existing = task(1L, TaskStatus.TO_DO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        taskService.delete(1L);

        verify(taskRepository).delete(existing);
    }

    @Test
    void deleteUnknownTaskThrowsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.delete(99L));
    }

    private Task task(Long id, TaskStatus status) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Task " + id);
        task.setStatus(status);
        return task;
    }
}
