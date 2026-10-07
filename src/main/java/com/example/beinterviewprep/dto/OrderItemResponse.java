package com.example.beinterviewprep.dto;

import com.example.beinterviewprep.entity.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice());
    }
}
