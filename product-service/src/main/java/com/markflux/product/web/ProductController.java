package com.markflux.product.web;

import com.markflux.common.web.PageResponse;
import com.markflux.product.service.ProductService;
import com.markflux.product.web.dto.ChangeProductStatusRequest;
import com.markflux.product.web.dto.CreateProductRequest;
import com.markflux.product.web.dto.ProductResponse;
import com.markflux.product.web.dto.ProductSearchCriteria;
import com.markflux.product.web.dto.UpdateProductRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/products/" + product.id())).body(product);
    }

    @GetMapping
    public PageResponse<ProductResponse> searchProducts(@Valid @ParameterObject ProductSearchCriteria criteria,
            @ParameterObject @PageableDefault(sort = "name") Pageable pageable, HttpServletRequest request) {
        ProductSearchCriteria visibleCriteria = request.isUserInRole("ADMIN") ? criteria : criteria.onlyActive();
        return PageResponse.from(productService.searchProducts(visibleCriteria, pageable));
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable UUID productId) {
        return productService.getProduct(productId);
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(@PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(productId, request);
    }

    @PatchMapping("/{productId}/status")
    public ProductResponse changeStatus(@PathVariable UUID productId,
            @Valid @RequestBody ChangeProductStatusRequest request) {
        return productService.changeStatus(productId, request.status());
    }
}
