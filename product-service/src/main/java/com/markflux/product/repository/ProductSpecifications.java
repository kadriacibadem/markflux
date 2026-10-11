package com.markflux.product.repository;

import com.markflux.product.domain.Product;
import com.markflux.product.web.dto.ProductSearchCriteria;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> matching(ProductSearchCriteria criteria) {
        List<Specification<Product>> specs = new ArrayList<>();
        if (criteria.q() != null && !criteria.q().isBlank()) {
            String pattern = "%" + escapeLike(criteria.q().trim().toLowerCase(Locale.ROOT)) + "%";
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
        }
        if (criteria.categoryId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("categoryId"), criteria.categoryId()));
        }
        if (criteria.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.minPrice() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
        }
        if (criteria.maxPrice() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
        }
        if (criteria.ids() != null && !criteria.ids().isEmpty()) {
            specs.add((root, query, cb) -> root.get("id").in(criteria.ids()));
        }
        return Specification.allOf(specs);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
