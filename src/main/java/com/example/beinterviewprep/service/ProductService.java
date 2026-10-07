package com.example.beinterviewprep.service;

import com.example.beinterviewprep.dto.PageResponse;
import com.example.beinterviewprep.dto.ProductFilter;
import com.example.beinterviewprep.dto.ProductRequest;
import com.example.beinterviewprep.dto.ProductResponse;
import com.example.beinterviewprep.entity.Product;
import com.example.beinterviewprep.exception.InvalidQueryException;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.repository.ProductRepository;
import com.example.beinterviewprep.repository.ProductSpecifications;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    public static final String CACHE = "products";

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "category", "price", "stock", "rating", "createdAt");

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(ProductFilter filter, Pageable pageable) {
        validate(filter, pageable);
        return PageResponse.from(
                productRepository.findAll(ProductSpecifications.from(filter), pageable).map(ProductResponse::from));
    }

    @Cacheable(value = CACHE, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @CacheEvict(value = CACHE, key = "#id")
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @CacheEvict(value = CACHE, key = "#id")
    @Transactional
    public void delete(Long id) {
        productRepository.delete(find(id));
    }

    private Product find(Long id) {
        return productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setRating(request.rating());
    }

    private void validate(ProductFilter filter, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidQueryException("Cannot sort by '" + order.getProperty() + "'");
            }
        }
        if (filter.minPrice() != null
                && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new InvalidQueryException("minPrice cannot be greater than maxPrice");
        }
    }
}
