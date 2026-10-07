package com.example.beinterviewprep.service;

import com.example.beinterviewprep.dto.OrderItemRequest;
import com.example.beinterviewprep.dto.OrderRequest;
import com.example.beinterviewprep.dto.OrderResponse;
import com.example.beinterviewprep.entity.Order;
import com.example.beinterviewprep.entity.OrderItem;
import com.example.beinterviewprep.entity.OrderStatus;
import com.example.beinterviewprep.entity.Product;
import com.example.beinterviewprep.exception.BusinessRuleException;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.repository.OrderRepository;
import com.example.beinterviewprep.repository.ProductRepository;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;

    public OrderResponse place(String customerEmail, String idempotencyKey, OrderRequest request) {
        return orderRepository
                .findByCustomerEmailAndIdempotencyKey(customerEmail, idempotencyKey)
                .map(OrderResponse::from)
                .orElseGet(() -> placeNew(customerEmail, idempotencyKey, request));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(String customerEmail, Long id) {
        return OrderResponse.from(find(customerEmail, id));
    }

    @Transactional
    public OrderResponse cancel(String customerEmail, Long id) {
        Order order = find(customerEmail, id);
        if (orderRepository.transition(id, OrderStatus.PLACED, OrderStatus.CANCELLED) == 1) {
            for (OrderItem item : order.getItems()) {
                productRepository.releaseStock(item.getProductId(), item.getQuantity());
                evictAfterCommit(item.getProductId());
            }
        }
        return OrderResponse.from(find(customerEmail, id));
    }

    private OrderResponse placeNew(String customerEmail, String idempotencyKey, OrderRequest request) {
        try {
            return transactionTemplate.execute(status -> reserve(customerEmail, idempotencyKey, request));
        } catch (DataIntegrityViolationException ex) {
            return orderRepository
                    .findByCustomerEmailAndIdempotencyKey(customerEmail, idempotencyKey)
                    .map(OrderResponse::from)
                    .orElseThrow(() -> ex);
        }
    }

    private OrderResponse reserve(String customerEmail, String idempotencyKey, OrderRequest request) {
        Order order = new Order();
        order.setCustomerEmail(customerEmail);
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(OrderStatus.PLACED);
        for (Map.Entry<Long, Long> entry : mergeQuantities(request).entrySet()) {
            Product product = productRepository
                    .findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + entry.getKey() + " not found"));
            order.addItem(toItem(product, entry.getValue()));
        }
        Order saved = orderRepository.saveAndFlush(order);
        for (OrderItem item : saved.getItems()) {
            if (productRepository.reserveStock(item.getProductId(), item.getQuantity()) == 0) {
                throw new BusinessRuleException("Insufficient stock for product " + item.getProductId());
            }
            evictAfterCommit(item.getProductId());
        }
        return OrderResponse.from(saved);
    }

    private OrderItem toItem(Product product, long quantity) {
        if (quantity > Integer.MAX_VALUE) {
            throw new BusinessRuleException("Insufficient stock for product " + product.getId());
        }
        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setQuantity((int) quantity);
        return item;
    }

    private Map<Long, Long> mergeQuantities(OrderRequest request) {
        Map<Long, Long> quantities = new TreeMap<>();
        for (OrderItemRequest item : request.items()) {
            quantities.merge(item.productId(), (long) item.quantity(), Long::sum);
        }
        return quantities;
    }

    private Order find(String customerEmail, Long id) {
        return orderRepository
                .findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " not found"));
    }

    private void evictAfterCommit(Long productId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Cache cache = cacheManager.getCache(ProductService.CACHE);
                if (cache != null) {
                    cache.evict(productId);
                }
            }
        });
    }
}
