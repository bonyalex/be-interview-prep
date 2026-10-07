package com.example.beinterviewprep.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.dto.ShortenRequest;
import com.example.beinterviewprep.dto.UrlStatsResponse;
import com.example.beinterviewprep.entity.ShortUrl;
import com.example.beinterviewprep.exception.ResourceExpiredException;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.repository.ShortUrlRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortCodeGenerator codeGenerator;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(shortUrlRepository, codeGenerator, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shortenStoresUrlWithGeneratedCodeAndExpiry() {
        Instant expiry = NOW.plusSeconds(3600);
        when(codeGenerator.next()).thenReturn("abc12345");
        when(shortUrlRepository.existsByCode("abc12345")).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl saved = service.shorten(new ShortenRequest("https://example.com/long", expiry));

        assertEquals("abc12345", saved.getCode());
        assertEquals("https://example.com/long", saved.getOriginalUrl());
        assertEquals(expiry, saved.getExpiresAt());
    }

    @Test
    void shortenRetriesWhenGeneratedCodeAlreadyExists() {
        when(codeGenerator.next()).thenReturn("taken123", "free4567");
        when(shortUrlRepository.existsByCode("taken123")).thenReturn(true);
        when(shortUrlRepository.existsByCode("free4567")).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl saved = service.shorten(new ShortenRequest("https://example.com", null));

        assertEquals("free4567", saved.getCode());
    }

    @Test
    void shorteningSameUrlTwiceCreatesTwoDistinctCodes() {
        when(codeGenerator.next()).thenReturn("first111", "second22");
        when(shortUrlRepository.existsByCode(any())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ShortenRequest request = new ShortenRequest("https://example.com", null);

        ShortUrl first = service.shorten(request);
        ShortUrl second = service.shorten(request);

        assertNotEquals(first.getCode(), second.getCode());
    }

    @Test
    void resolveReturnsOriginalUrlAndCountsTheVisit() {
        ShortUrl shortUrl = shortUrl("abc12345", null);
        when(shortUrlRepository.findByCode("abc12345")).thenReturn(Optional.of(shortUrl));

        String target = service.resolve("abc12345");

        assertEquals("https://example.com/long", target);
        verify(shortUrlRepository).incrementVisitCount(7L);
    }

    @Test
    void resolveUnknownCodeThrowsNotFound() {
        when(shortUrlRepository.findByCode("nope")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resolve("nope"));
    }

    @Test
    void resolveExpiredCodeThrowsExpiredAndDoesNotCountTheVisit() {
        ShortUrl expired = shortUrl("old12345", NOW.minusSeconds(1));
        when(shortUrlRepository.findByCode("old12345")).thenReturn(Optional.of(expired));

        assertThrows(ResourceExpiredException.class, () -> service.resolve("old12345"));

        verify(shortUrlRepository, never()).incrementVisitCount(any());
    }

    @Test
    void statsReturnsOriginalUrlVisitCountAndCreatedDate() {
        ShortUrl shortUrl = shortUrl("abc12345", null);
        shortUrl.setVisitCount(3);
        shortUrl.setCreatedAt(NOW);
        when(shortUrlRepository.findByCode("abc12345")).thenReturn(Optional.of(shortUrl));

        UrlStatsResponse stats = service.stats("abc12345");

        assertEquals("https://example.com/long", stats.originalUrl());
        assertEquals(3, stats.visitCount());
        assertEquals(NOW, stats.createdAt());
    }

    @Test
    void generatedCodesAreAtMostEightUrlSafeCharacters() {
        ShortCodeGenerator generator = new ShortCodeGenerator();

        for (int i = 0; i < 500; i++) {
            String code = generator.next();
            assertTrue(code.length() <= 8);
            assertTrue(code.matches("[A-Za-z0-9]+"));
        }
    }

    private ShortUrl shortUrl(String code, Instant expiresAt) {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setId(7L);
        shortUrl.setCode(code);
        shortUrl.setOriginalUrl("https://example.com/long");
        shortUrl.setExpiresAt(expiresAt);
        return shortUrl;
    }
}
