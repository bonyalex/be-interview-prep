package com.example.beinterviewprep.repository;

import com.example.beinterviewprep.dto.ProductFilter;
import com.example.beinterviewprep.entity.Product;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private ProductSpecifications() {}

    public static Specification<Product> from(ProductFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(filter.category())) {
                predicates.add(builder.equal(
                        builder.lower(root.get("category")),
                        filter.category().trim().toLowerCase(Locale.ROOT)));
            }
            if (filter.minPrice() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }
            if (filter.inStockOnly()) {
                predicates.add(builder.greaterThan(root.get("stock"), 0));
            }
            if (StringUtils.hasText(filter.search())) {
                predicates.add(builder.like(
                        builder.lower(root.get("name")),
                        "%" + escape(filter.search().trim().toLowerCase(Locale.ROOT)) + "%",
                        ESCAPE));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
