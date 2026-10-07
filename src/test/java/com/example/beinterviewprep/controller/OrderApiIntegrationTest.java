package com.example.beinterviewprep.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.entity.Product;
import com.example.beinterviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

    private static final String BUYER = "buyer@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void placingAnOrderRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(BUYER)
    void placedOrderIsCreatedWithItemsAndTotal() throws Exception {
        Product product = productWithStock(5);

        mockMvc.perform(order(product.getId(), 2, UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(19.98));
    }

    @Test
    @WithMockUser(BUYER)
    void insufficientStockReturns409WithAClearMessage() throws Exception {
        Product product = productWithStock(1);

        mockMvc.perform(order(product.getId(), 2, UUID.randomUUID().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product " + product.getId()));
    }

    @Test
    @WithMockUser(BUYER)
    void retryReturnsTheSameOrder() throws Exception {
        Product product = productWithStock(5);
        String key = UUID.randomUUID().toString();
        String first = mockMvc.perform(order(product.getId(), 2, key))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String firstId = first.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(order(product.getId(), 2, key))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(Long.parseLong(firstId)));

        mockMvc.perform(get("/api/products/" + product.getId())).andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    @WithMockUser(BUYER)
    void missingIdempotencyKeyReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(BUYER)
    void emptyItemsAndZeroQuantityReturn400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(BUYER)
    void unknownProductReturns404() throws Exception {
        mockMvc.perform(order(999999L, 1, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(BUYER)
    void cancellingReturnsStockAndMarksTheOrderCancelled() throws Exception {
        Product product = productWithStock(5);
        String body = mockMvc.perform(order(product.getId(), 5, UUID.randomUUID().toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mockMvc.perform(get("/api/products/" + product.getId())).andExpect(jsonPath("$.stock").value(0));

        mockMvc.perform(post("/api/orders/" + id + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/products/" + product.getId())).andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    @WithMockUser("someone-else@example.com")
    void anotherCustomersOrderReturns404() throws Exception {
        Product product = productWithStock(5);
        String body = mockMvc.perform(order(product.getId(), 1, UUID.randomUUID().toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/orders/" + id).with(user("intruder@example.com")))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder order(Long productId, int quantity, String key) {
        return post("/api/orders")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]}");
    }

    private Product productWithStock(int stock) {
        Product product = new Product();
        product.setName("Widget " + UUID.randomUUID());
        product.setCategory("Test");
        product.setPrice(new BigDecimal("9.99"));
        product.setStock(stock);
        product.setRating(4.0);
        return productRepository.save(product);
    }
}
