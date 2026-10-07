package com.example.beinterviewprep.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import com.example.beinterviewprep.dto.OrderRequest;
import com.example.beinterviewprep.dto.OrderResponse;
import com.example.beinterviewprep.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Orders")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order idempotently")
    public OrderResponse place(
            Principal principal,
            @Parameter(description = "Unique per customer; a retry with the same key returns the original order")
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {
        return orderService.place(principal.getName(), idempotencyKey.trim(), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the caller's orders")
    public OrderResponse get(Principal principal, @PathVariable Long id) {
        return orderService.get(principal.getName(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order and return its stock")
    public OrderResponse cancel(Principal principal, @PathVariable Long id) {
        return orderService.cancel(principal.getName(), id);
    }
}
