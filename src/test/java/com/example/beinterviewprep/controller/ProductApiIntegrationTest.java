package com.example.beinterviewprep.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void pageSizeIsCappedAtOneHundred() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content.length()").value(100));
    }

    @Test
    @WithMockUser
    void listFiltersSortsAndReportsTotals() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("category", "Books")
                        .param("inStockOnly", "true")
                        .param("minPrice", "10")
                        .param("search", "product")
                        .param("sort", "price,desc")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @WithMockUser
    void unknownSortFieldReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "secret,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser
    void unknownProductReturns404() throws Exception {
        mockMvc.perform(get("/api/products/999999")).andExpect(status().isNotFound());
    }
}
