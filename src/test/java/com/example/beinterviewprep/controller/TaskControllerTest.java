package com.example.beinterviewprep.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.service.TaskService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createWithInvalidFieldsReturns400WithMessagePerField() throws Exception {
        String longTitle = "a".repeat(101);
        String yesterday = LocalDate.now().minusDays(1).toString();
        String body = "{\"title\":\"" + longTitle + "\",\"dueDate\":\"" + yesterday + "\"}";

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[?(@ =~ /title:.*/)]").isNotEmpty())
                .andExpect(jsonPath("$.details[?(@ =~ /dueDate:.*/)]").isNotEmpty());
    }

    @Test
    void createWithBlankTitleReturns400() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("title: title is required"));
    }

    @Test
    void getUnknownTaskReturns404InErrorShape() throws Exception {
        when(taskService.get(99L)).thenThrow(new ResourceNotFoundException("Task 99 not found"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task 99 not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/99"));
    }

    @Test
    void listWithUnknownStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("status: invalid value 'BOGUS'"));
    }

    @Test
    void unexpectedErrorReturns500InErrorShape() throws Exception {
        when(taskService.get(1L)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}
