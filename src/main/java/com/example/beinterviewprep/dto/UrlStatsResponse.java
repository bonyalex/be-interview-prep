package com.example.beinterviewprep.dto;

import com.example.beinterviewprep.entity.ShortUrl;
import java.time.Instant;

public record UrlStatsResponse(String code, String originalUrl, long visitCount, Instant createdAt) {

    public static UrlStatsResponse from(ShortUrl shortUrl) {
        return new UrlStatsResponse(
                shortUrl.getCode(), shortUrl.getOriginalUrl(), shortUrl.getVisitCount(), shortUrl.getCreatedAt());
    }
}
