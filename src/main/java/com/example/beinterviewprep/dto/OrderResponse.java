package com.example.beinterviewprep.dto;

import com.example.beinterviewprep.entity.Order;
import com.example.beinterviewprep.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id, OrderStatus status, List<OrderItemResponse> items, BigDecimal total, Instant createdAt) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items =
                order.getItems().stream().map(OrderItemResponse::from).toList();
        BigDecimal total = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(order.getId(), order.getStatus(), items, total, order.getCreatedAt());
    }
}
