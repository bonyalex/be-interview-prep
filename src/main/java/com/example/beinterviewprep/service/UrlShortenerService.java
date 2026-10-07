package com.example.beinterviewprep.service;

import com.example.beinterviewprep.dto.ShortenRequest;
import com.example.beinterviewprep.dto.UrlStatsResponse;
import com.example.beinterviewprep.entity.ShortUrl;
import com.example.beinterviewprep.exception.ResourceExpiredException;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.repository.ShortUrlRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator codeGenerator;
    private final Clock clock;

    @Transactional
    public ShortUrl shorten(ShortenRequest request) {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setCode(uniqueCode());
        shortUrl.setOriginalUrl(request.url());
        shortUrl.setExpiresAt(request.expiresAt());
        return shortUrlRepository.save(shortUrl);
    }

    @Transactional
    public String resolve(String code) {
        ShortUrl shortUrl = find(code);
        if (isExpired(shortUrl)) {
            throw new ResourceExpiredException("Short code " + code + " has expired");
        }
        shortUrlRepository.incrementVisitCount(shortUrl.getId());
        return shortUrl.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse stats(String code) {
        return UrlStatsResponse.from(find(code));
    }

    private ShortUrl find(String code) {
        return shortUrlRepository
                .findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Short code " + code + " not found"));
    }

    private boolean isExpired(ShortUrl shortUrl) {
        Instant expiresAt = shortUrl.getExpiresAt();
        return expiresAt != null && !expiresAt.isAfter(Instant.now(clock));
    }

    private String uniqueCode() {
        String code = codeGenerator.next();
        while (shortUrlRepository.existsByCode(code)) {
            code = codeGenerator.next();
        }
        return code;
    }
}
