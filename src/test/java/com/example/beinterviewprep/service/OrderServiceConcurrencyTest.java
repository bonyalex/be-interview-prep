package com.example.beinterviewprep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.beinterviewprep.dto.OrderItemRequest;
import com.example.beinterviewprep.dto.OrderRequest;
import com.example.beinterviewprep.dto.OrderResponse;
import com.example.beinterviewprep.entity.OrderStatus;
import com.example.beinterviewprep.entity.Product;
import com.example.beinterviewprep.exception.BusinessRuleException;
import com.example.beinterviewprep.repository.OrderRepository;
import com.example.beinterviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceConcurrencyTest {

    private static final String CUSTOMER = "buyer@example.com";

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void fiftySimultaneousOrdersForStockOfTenSucceedExactlyTenTimes() throws Exception {
        Product product = productWithStock(10);
        int attempts = 50;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            String key = UUID.randomUUID().toString();
            Callable<Void> attempt = () -> {
                ready.countDown();
                start.await();
                try {
                    orderService.place(CUSTOMER, key, request(product.getId(), 1));
                    succeeded.incrementAndGet();
                } catch (BusinessRuleException ex) {
                    rejected.incrementAndGet();
                }
                return null;
            };
            futures.add(executor.submit(attempt));
        }
        ready.await();
        start.countDown();
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertThat(succeeded.get()).isEqualTo(10);
        assertThat(rejected.get()).isEqualTo(40);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isZero();
    }

    @Test
    void retryWithTheSameKeyCreatesOneOrderAndReservesStockOnce() {
        Product product = productWithStock(10);
        String key = UUID.randomUUID().toString();

        OrderResponse first = orderService.place(CUSTOMER, key, request(product.getId(), 3));
        OrderResponse retry = orderService.place(CUSTOMER, key, request(product.getId(), 3));

        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(orderRepository.findAll().stream()
                        .filter(order -> order.getIdempotencyKey().equals(key))
                        .count())
                .isEqualTo(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(7);
    }

    @Test
    void simultaneousRetriesWithTheSameKeyCreateOneOrder() throws Exception {
        Product product = productWithStock(10);
        String key = UUID.randomUUID().toString();
        int attempts = 10;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Future<OrderResponse>> futures = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return orderService.place(CUSTOMER, key, request(product.getId(), 2));
            }));
        }
        start.countDown();
        List<Long> orderIds = new ArrayList<>();
        for (Future<OrderResponse> future : futures) {
            orderIds.add(future.get().id());
        }
        executor.shutdown();

        assertThat(orderIds.stream().distinct().count()).isEqualTo(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(8);
    }

    @Test
    void insufficientStockOnOneItemRollsBackTheWholeOrder() {
        Product plentiful = productWithStock(10);
        Product scarce = productWithStock(1);
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(plentiful.getId(), 4), new OrderItemRequest(scarce.getId(), 2)));
        String key = UUID.randomUUID().toString();

        assertThatThrownBy(() -> orderService.place(CUSTOMER, key, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(String.valueOf(scarce.getId()));

        assertThat(productRepository.findById(plentiful.getId()).orElseThrow().getStock())
                .isEqualTo(10);
        assertThat(productRepository.findById(scarce.getId()).orElseThrow().getStock())
                .isEqualTo(1);
        assertThat(orderRepository.findByCustomerEmailAndIdempotencyKey(CUSTOMER, key))
                .isEmpty();
    }

    @Test
    void cancellingReturnsStockOnlyOnce() {
        Product product = productWithStock(10);
        OrderResponse order = orderService.place(CUSTOMER, UUID.randomUUID().toString(), request(product.getId(), 4));

        OrderResponse cancelled = orderService.cancel(CUSTOMER, order.id());
        OrderResponse cancelledAgain = orderService.cancel(CUSTOMER, order.id());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledAgain.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
                .isEqualTo(10);
    }

    @Test
    void cachedProductShowsTheNewStockAfterAnOrder() {
        Product product = productWithStock(10);
        productService.get(product.getId());

        orderService.place(CUSTOMER, UUID.randomUUID().toString(), request(product.getId(), 6));

        assertThat(productService.get(product.getId()).stock()).isEqualTo(4);
    }

    private Product productWithStock(int stock) {
        Product product = new Product();
        product.setName("Widget " + UUID.randomUUID());
        product.setCategory("Test");
        product.setPrice(new BigDecimal("9.99"));
        product.setStock(stock);
        product.setRating(4.0);
        return productRepository.save(product);
    }

    private OrderRequest request(Long productId, int quantity) {
        return new OrderRequest(List.of(new OrderItemRequest(productId, quantity)));
    }
}
