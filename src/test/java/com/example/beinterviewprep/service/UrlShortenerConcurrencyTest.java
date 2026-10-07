package com.example.beinterviewprep.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.beinterviewprep.dto.ShortenRequest;
import com.example.beinterviewprep.entity.ShortUrl;
import com.example.beinterviewprep.repository.ShortUrlRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UrlShortenerConcurrencyTest {

    private static final int VISITS = 50;

    @Autowired
    private UrlShortenerService service;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Test
    void concurrentVisitsAreAllCounted() throws Exception {
        ShortUrl shortUrl = service.shorten(new ShortenRequest("https://example.com/busy", null));
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> visits = new ArrayList<>();

        for (int i = 0; i < VISITS; i++) {
            visits.add(executor.submit(() -> {
                start.await();
                return service.resolve(shortUrl.getCode());
            }));
        }
        start.countDown();
        for (Future<String> visit : visits) {
            visit.get();
        }
        executor.shutdown();

        long counted = shortUrlRepository.findByCode(shortUrl.getCode()).orElseThrow().getVisitCount();
        assertEquals(VISITS, counted);
    }
}
