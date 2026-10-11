package com.markflux.order.client;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "cart-service", url = "${markflux.clients.cart-service.url}")
public interface CartClient {

    @GetMapping("/api/cart")
    Cart getCart();

    record Cart(List<Item> items) {

        public record Item(UUID productId, int quantity) {
        }
    }
}
