package com.example.beinterviewprep.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.dto.UrlStatsResponse;
import com.example.beinterviewprep.entity.ShortUrl;
import com.example.beinterviewprep.exception.ResourceExpiredException;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.security.JsonSecurityErrorHandler;
import com.example.beinterviewprep.service.JwtService;
import com.example.beinterviewprep.service.UrlShortenerService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UrlShortenerController.class)
@AutoConfigureMockMvc(addFilters = false)
class UrlShortenerControllerTest {

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JsonSecurityErrorHandler errorHandler;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlShortenerService urlShortenerService;

    @Test
    void shortenReturnsCodeAndShortUrl() throws Exception {
        ShortUrl saved = new ShortUrl();
        saved.setCode("abc12345");
        saved.setOriginalUrl("https://example.com/long");
        when(urlShortenerService.shorten(ArgumentMatchers.any())).thenReturn(saved);
        String body = "{\"url\":\"https://example.com/long\"}";

        mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("abc12345"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/abc12345"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/long"));
    }

    @Test
    void shortenRejectsInvalidUrlWithFieldMessage() throws Exception {
        String body = "{\"url\":\"not a url\"}";

        mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("url: url must be a valid http or https URL"));
    }

    @Test
    void shortenRejectsNonHttpScheme() throws Exception {
        String body = "{\"url\":\"javascript:alert(1)\"}";

        mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenRejectsExpiryInThePast() throws Exception {
        String body = "{\"url\":\"https://example.com\",\"expiresAt\":\"2020-01-01T00:00:00Z\"}";

        mockMvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("expiresAt: expiresAt must be in the future"));
    }

    @Test
    void visitingShortUrlRedirectsToOriginal() throws Exception {
        when(urlShortenerService.resolve("abc12345")).thenReturn("https://example.com/long");

        mockMvc.perform(get("/abc12345"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/long"));
    }

    @Test
    void visitingUnknownCodeReturns404() throws Exception {
        when(urlShortenerService.resolve("nope")).thenThrow(new ResourceNotFoundException("Short code nope not found"));

        mockMvc.perform(get("/nope")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void visitingExpiredCodeReturns410() throws Exception {
        when(urlShortenerService.resolve("old")).thenThrow(new ResourceExpiredException("Short code old has expired"));

        mockMvc.perform(get("/old")).andExpect(status().isGone()).andExpect(jsonPath("$.status").value(410));
    }

    @Test
    void statsReturnsUrlVisitCountAndCreatedDate() throws Exception {
        Instant createdAt = Instant.parse("2026-10-07T10:00:00Z");
        when(urlShortenerService.stats("abc12345"))
                .thenReturn(new UrlStatsResponse("abc12345", "https://example.com/long", 4, createdAt));

        mockMvc.perform(get("/api/urls/abc12345/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/long"))
                .andExpect(jsonPath("$.visitCount").value(4))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void statsForUnknownCodeReturns404() throws Exception {
        when(urlShortenerService.stats("nope")).thenThrow(new ResourceNotFoundException("Short code nope not found"));

        mockMvc.perform(get("/api/urls/nope/stats")).andExpect(status().isNotFound());
    }
}
