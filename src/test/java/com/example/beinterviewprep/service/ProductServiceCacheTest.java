package com.example.beinterviewprep.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.beinterviewprep.dto.ProductRequest;
import com.example.beinterviewprep.dto.ProductResponse;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class ProductServiceCacheTest {

    @Autowired
    private ProductService service;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private ProductRepository productRepository;

    private Long productId;

    @BeforeEach
    void createProduct() {
        ProductResponse created = service.create(new ProductRequest("Cached", "Books", new BigDecimal("10.00"), 5, 4.0));
        productId = created.id();
        cacheManager.getCache(ProductService.CACHE).clear();
        clearInvocations(productRepository);
    }

    @Test
    void repeatedLookupsQueryTheDatabaseOnce() {
        ProductResponse first = service.get(productId);
        ProductResponse second = service.get(productId);
        ProductResponse third = service.get(productId);

        verify(productRepository, times(1)).findById(productId);
        assertSame(first, second);
        assertSame(first, third);
    }

    @Test
    void updateEvictsTheCachedProduct() {
        service.get(productId);

        service.update(productId, new ProductRequest("Renamed", "Books", new BigDecimal("12.50"), 3, 4.5));
        ProductResponse afterUpdate = service.get(productId);

        assertEquals("Renamed", afterUpdate.name());
        assertEquals(new BigDecimal("12.50"), afterUpdate.price());
    }

    @Test
    void deleteEvictsTheCachedProduct() {
        service.get(productId);

        service.delete(productId);

        assertThrows(ResourceNotFoundException.class, () -> service.get(productId));
    }
}
