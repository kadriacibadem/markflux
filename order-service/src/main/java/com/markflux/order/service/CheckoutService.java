package com.markflux.order.service;

import com.markflux.common.error.BusinessException;
import com.markflux.common.error.ServiceUnavailableException;
import com.markflux.order.client.CartClient;
import com.markflux.order.client.ProductClient;
import com.markflux.order.domain.Order;
import com.markflux.order.domain.OrderLine;
import com.markflux.order.messaging.OrderEventPublisher;
import com.markflux.order.repository.OrderRepository;
import com.markflux.order.web.dto.OrderResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final OrderEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final String currency;

    public CheckoutService(OrderRepository orderRepository, CartClient cartClient, ProductClient productClient,
            OrderEventPublisher eventPublisher, TransactionTemplate transactionTemplate,
            @Value("${markflux.order.currency:TRY}") String currency) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.productClient = productClient;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.currency = currency;
    }

    public CheckoutResult checkout(UUID userId, UUID idempotencyKey) {
        Optional<OrderResponse> existing = findExistingOrder(userId, idempotencyKey);
        if (existing.isPresent()) {
            return new CheckoutResult(existing.get(), true);
        }

        List<OrderLine> lines = createOrderLines();
        try {
            // create order
            OrderResponse order = transactionTemplate.execute(status -> {
                Order saved = orderRepository.saveAndFlush(new Order(userId, idempotencyKey, lines, currency));
                eventPublisher.publish("OrderCreated", saved, null);
                return OrderResponse.from(saved);
            });
            return new CheckoutResult(order, false);
        } catch (DataIntegrityViolationException ex) {
            return findExistingOrder(userId, idempotencyKey)
                    .map(order -> new CheckoutResult(order, true))
                    .orElseThrow(() -> ex);
        }
    }

    private List<OrderLine> createOrderLines() {
        List<CartClient.Cart.Item> cartItems = callRemote("CART_SERVICE_UNAVAILABLE", cartClient::getCart).items();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException("CART_EMPTY", "The cart is empty");
        }

        // request to product service for items
        List<UUID> productIds = cartItems.stream().map(CartClient.Cart.Item::productId).toList();
        Map<UUID, ProductClient.Product> products = new HashMap<>();
        callRemote("PRODUCT_SERVICE_UNAVAILABLE", () -> productClient.getProducts(productIds, productIds.size()))
                .content()
                .forEach(product -> products.put(product.id(), product));

        // check unavilable products
        List<UUID> unavailable = productIds.stream()
                .filter(id -> !products.containsKey(id) || !products.get(id).isActive())
                .toList();
        if (!unavailable.isEmpty()) {
            throw new BusinessException("PRODUCTS_UNAVAILABLE", "Some products in the cart are no longer available",
                    Map.of("productIds", unavailable));
        }

        // create orderline for exist products
        return cartItems.stream()
                .map(item -> {
                    ProductClient.Product product = products.get(item.productId());
                    return new OrderLine(product.id(), product.name(), product.price(), item.quantity());
                })
                .toList();
    }

    private Optional<OrderResponse> findExistingOrder(UUID userId, UUID idempotencyKey) {
        return transactionTemplate.execute(status -> orderRepository
                .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(OrderResponse::from));
    }

    private static <T> T callRemote(String errorCode, Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException ex) {
            throw new ServiceUnavailableException(errorCode, "A required service is not available, please retry",
                    ex);
        }
    }

    public record CheckoutResult(OrderResponse order, boolean replayed) {
    }
}
