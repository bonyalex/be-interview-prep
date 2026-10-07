package com.example.beinterviewprep.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.beinterviewprep.dto.ShortenRequest;
import com.example.beinterviewprep.dto.ShortenResponse;
import com.example.beinterviewprep.dto.UrlStatsResponse;
import com.example.beinterviewprep.entity.ShortUrl;
import com.example.beinterviewprep.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "URL shortener")
@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/api/urls")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a short URL")
    public ShortenResponse shorten(@Valid @RequestBody ShortenRequest request) {
        ShortUrl shortUrl = urlShortenerService.shorten(request);
        String shortLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(shortUrl.getCode())
                .toUriString();
        return new ShortenResponse(shortUrl.getCode(), shortLink, shortUrl.getOriginalUrl(), shortUrl.getExpiresAt());
    }

    @GetMapping("/api/urls/{code}/stats")
    @Operation(summary = "Get visit statistics for a short code")
    public UrlStatsResponse stats(@PathVariable String code) {
        return urlShortenerService.stats(code);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Redirect a short code to its original URL")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String target = urlShortenerService.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, target)
                .build();
    }
}
