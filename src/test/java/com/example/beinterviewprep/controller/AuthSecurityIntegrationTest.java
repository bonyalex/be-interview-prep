package com.example.beinterviewprep.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.config.JwtProperties;
import com.example.beinterviewprep.entity.AppUser;
import com.example.beinterviewprep.entity.Role;
import com.example.beinterviewprep.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    private static final String BEARER = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    private String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, password)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    @Test
    void registeredUserLogsInAndViewsOwnProfile() throws Exception {
        register("profile@example.com", "s3cret-pass");
        String token = login("profile@example.com", "s3cret-pass");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void userRoleGets403OnAdminEndpoint() throws Exception {
        register("plain@example.com", "s3cret-pass");
        String token = login("plain@example.com", "s3cret-pass");

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void adminListsAllUsers() throws Exception {
        register("listed@example.com", "s3cret-pass");
        String token = login("admin@example.com", "admin-password-1");

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.email == 'listed@example.com')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.email == 'admin@example.com')].role").value("ADMIN"));
    }

    @Test
    void requestWithoutTokenGets401Json() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content -> {
                    String contentType = content.getResponse().getContentType();
                    assertTrue(contentType.startsWith(MediaType.APPLICATION_JSON_VALUE));
                })
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidTokenGets401Json() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, BEARER + "garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void expiredTokenGets401Json() throws Exception {
        Instant longAgo = Instant.now().minus(Duration.ofHours(1));
        JwtService pastService = new JwtService(jwtProperties, Clock.fixed(longAgo, ZoneOffset.UTC));
        AppUser user = new AppUser();
        user.setEmail("admin@example.com");
        user.setRole(Role.ADMIN);
        String expired = pastService.generate(user);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, BEARER + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void wrongPasswordGets401() throws Exception {
        register("wrongpass@example.com", "s3cret-pass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("wrongpass@example.com", "not-the-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void duplicateRegistrationGets409() throws Exception {
        register("dupe@example.com", "s3cret-pass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("dupe@example.com", "s3cret-pass")))
                .andExpect(status().isConflict());
    }

    @Test
    void shortPasswordOnRegisterGets400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("short@example.com", "short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherApiEndpointsRequireLogin() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }
}
