package com.markflux.cart.client;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${markflux.clients.product-service.url}", dismiss404 = true)
public interface ProductClient {

    @GetMapping("/api/products/{productId}")
    Optional<Product> getProduct(@PathVariable("productId") UUID productId);

    record Product(UUID id, String name, BigDecimal price, String status) {

        public boolean isActive() {
            return "ACTIVE".equals(status);
        }
    }
}
