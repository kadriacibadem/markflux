package com.markflux.order.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "${markflux.clients.product-service.url}")
public interface ProductClient {

    @GetMapping("/api/products")
    ProductPage getProducts(@RequestParam("ids") List<UUID> ids, @RequestParam("size") int size);

    record ProductPage(List<Product> content) {
    }

    record Product(UUID id, String name, BigDecimal price, String status) {

        public boolean isActive() {
            return "ACTIVE".equals(status);
        }
    }
}
