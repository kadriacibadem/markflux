package com.markflux.product.service;

import com.markflux.common.error.BusinessException;
import com.markflux.common.error.ConflictException;
import com.markflux.common.error.NotFoundException;
import com.markflux.product.config.CacheConfig;
import com.markflux.product.domain.Product;
import com.markflux.product.domain.ProductStatus;
import com.markflux.product.messaging.ProductEventPublisher;
import com.markflux.product.repository.CategoryRepository;
import com.markflux.product.repository.ProductRepository;
import com.markflux.product.repository.ProductSpecifications;
import com.markflux.product.web.dto.CreateProductRequest;
import com.markflux.product.web.dto.ProductResponse;
import com.markflux.product.web.dto.ProductSearchCriteria;
import com.markflux.product.web.dto.UpdateProductRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductEventPublisher eventPublisher;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ConflictException("PRODUCT_SKU_ALREADY_EXISTS", "SKU already exists: " + request.sku());
        }
        checkCategoryExists(request.categoryId());
        ProductStatus status = request.status() != null ? request.status() : ProductStatus.ACTIVE;
        Product product = productRepository.save(new Product(request.sku(), request.name(), request.description(),
                request.price(), request.categoryId(), status));
        eventPublisher.publish("ProductCreated", product);
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#productId")
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = findProduct(productId);
        checkCategoryExists(request.categoryId());
        product.update(request.name(), request.description(), request.price(), request.categoryId());
        eventPublisher.publish("ProductUpdated", product);
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#productId")
    public ProductResponse changeStatus(UUID productId, ProductStatus status) {
        Product product = findProduct(productId);
        if (product.changeStatus(status)) {
            eventPublisher.publish("ProductStatusChanged", product);
        }
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.PRODUCTS, key = "#productId")
    public ProductResponse getProduct(UUID productId) {
        return ProductResponse.from(findProduct(productId));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        return productRepository.findAll(ProductSpecifications.matching(criteria), pageable)
                .map(ProductResponse::from);
    }

    private Product findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + productId));
    }

    private void checkCategoryExists(UUID categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "Category not found: " + categoryId);
        }
    }
}
