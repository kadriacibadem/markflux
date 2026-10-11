package com.markflux.order.web;

import com.markflux.common.security.AuthenticatedUser;
import com.markflux.common.web.PageResponse;
import com.markflux.order.service.CheckoutService.CheckoutResult;
import com.markflux.order.service.CheckoutService;
import com.markflux.order.service.OrderService;
import com.markflux.order.web.dto.OrderResponse;
import com.markflux.order.web.dto.OrderSummaryResponse;
import com.markflux.order.web.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey) {
        CheckoutResult result = checkoutService.checkout(user.id(), idempotencyKey);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .location(URI.create("/api/orders/" + result.order().id()))
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(result.order());
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(AuthenticatedUser user, @PathVariable UUID orderId) {
        return orderService.getOrder(user, orderId);
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> listOrders(AuthenticatedUser user,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(orderService.listOrders(user.id(), pageable));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(AuthenticatedUser user, @PathVariable UUID orderId) {
        return orderService.cancelOrder(user, orderId);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(@PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(orderId, request.status());
    }
}
