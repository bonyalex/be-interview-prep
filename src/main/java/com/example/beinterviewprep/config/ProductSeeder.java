package com.example.beinterviewprep.config;

import com.example.beinterviewprep.entity.Product;
import com.example.beinterviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSeeder implements ApplicationRunner {

    static final int SEED_COUNT = 100;

    private static final List<String> CATEGORIES = List.of("Electronics", "Books", "Clothing", "Home", "Toys");

    private final ProductRepository productRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= SEED_COUNT; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(CATEGORIES.get(i % CATEGORIES.size()));
            product.setPrice(BigDecimal.valueOf(5 + (i * 7L) % 495).setScale(2));
            product.setStock(i % 4 == 0 ? 0 : (i * 3) % 50 + 1);
            product.setRating((i % 11) / 2.0);
            products.add(product);
        }
        productRepository.saveAll(products);
        log.info("Seeded {} products", products.size());
    }
}
