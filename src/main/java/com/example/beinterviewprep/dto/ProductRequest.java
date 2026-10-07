package com.example.beinterviewprep.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "name is required") @Size(max = 150, message = "name must be at most 150 characters")
                String name,
        @NotBlank(message = "category is required")
                @Size(max = 100, message = "category must be at most 100 characters")
                String category,
        @NotNull(message = "price is required") @DecimalMin(value = "0.00", message = "price cannot be negative")
                BigDecimal price,
        @Min(value = 0, message = "stock cannot be negative") int stock,
        @DecimalMin(value = "0.0", message = "rating must be between 0 and 5")
                @DecimalMax(value = "5.0", message = "rating must be between 0 and 5")
                double rating) {}
