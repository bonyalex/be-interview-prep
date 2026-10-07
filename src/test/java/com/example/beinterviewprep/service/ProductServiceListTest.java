package com.example.beinterviewprep.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.beinterviewprep.dto.PageResponse;
import com.example.beinterviewprep.dto.ProductFilter;
import com.example.beinterviewprep.dto.ProductResponse;
import com.example.beinterviewprep.exception.InvalidQueryException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@SpringBootTest
class ProductServiceListTest {

    private static final ProductFilter NO_FILTER = new ProductFilter(null, null, null, false, null);

    @Autowired
    private ProductService service;

    @Test
    void firstPageReportsTotalCountAndPageCount() {
        PageResponse<ProductResponse> page = service.list(NO_FILTER, PageRequest.of(0, 30));

        assertEquals(30, page.content().size());
        assertTrue(page.totalElements() >= 100);
        assertEquals((int) Math.ceil(page.totalElements() / 30.0), page.totalPages());
    }

    @Test
    void sortsByAnyFieldInRequestedDirection() {
        PageResponse<ProductResponse> page =
                service.list(NO_FILTER, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "price")));

        List<BigDecimal> prices =
                page.content().stream().map(ProductResponse::price).toList();
        for (int i = 1; i < prices.size(); i++) {
            assertTrue(prices.get(i - 1).compareTo(prices.get(i)) >= 0);
        }
    }

    @Test
    void combinesAllFiltersInOneRequest() {
        ProductFilter filter =
                new ProductFilter("books", new BigDecimal("50"), new BigDecimal("300"), true, "PRODUCT");

        PageResponse<ProductResponse> page = service.list(filter, PageRequest.of(0, 100));

        assertTrue(page.totalElements() > 0);
        for (ProductResponse product : page.content()) {
            assertEquals("Books", product.category());
            assertTrue(product.price().compareTo(new BigDecimal("50")) >= 0);
            assertTrue(product.price().compareTo(new BigDecimal("300")) <= 0);
            assertTrue(product.stock() > 0);
            assertTrue(product.name().toLowerCase().contains("product"));
        }
    }

    @Test
    void nameSearchTreatsWildcardCharactersLiterally() {
        ProductFilter filter = new ProductFilter(null, null, null, false, "%");

        PageResponse<ProductResponse> page = service.list(filter, PageRequest.of(0, 10));

        assertEquals(0, page.totalElements());
    }

    @Test
    void unknownSortFieldIsRejected() {
        assertThrows(
                InvalidQueryException.class,
                () -> service.list(NO_FILTER, PageRequest.of(0, 10, Sort.by("password"))));
    }

    @Test
    void inconsistentPriceRangeIsRejected() {
        ProductFilter filter = new ProductFilter(null, new BigDecimal("100"), new BigDecimal("10"), false, null);

        assertThrows(InvalidQueryException.class, () -> service.list(filter, PageRequest.of(0, 10)));
    }
}
