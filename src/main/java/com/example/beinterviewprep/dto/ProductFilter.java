package com.example.beinterviewprep.dto;

import java.math.BigDecimal;

public record ProductFilter(
        String category, BigDecimal minPrice, BigDecimal maxPrice, boolean inStockOnly, String search) {}
