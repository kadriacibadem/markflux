package com.markflux.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.markflux.common.error.BusinessException;
import com.markflux.common.error.ConflictException;
import com.markflux.common.error.NotFoundException;
import com.markflux.product.domain.Product;
import com.markflux.product.domain.ProductStatus;
import com.markflux.product.messaging.ProductEventPublisher;
import com.markflux.product.repository.CategoryRepository;
import com.markflux.product.repository.ProductRepository;
import com.markflux.product.web.dto.CreateProductRequest;
import com.markflux.product.web.dto.UpdateProductRequest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductSavesAndPublishesEvent() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.createProduct(new CreateProductRequest("SKU-1", "Keyboard", null,
                new BigDecimal("999.90"), null, null));

        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);
        verify(eventPublisher).publish(any(), any(Product.class));
    }

    @Test
    void createProductRejectsDuplicateSku() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(new CreateProductRequest("SKU-1", "Keyboard", null,
                BigDecimal.TEN, null, null)))
                .isInstanceOf(ConflictException.class);
        verify(eventPublisher, never()).publish(anyString(), any());
    }

    @Test
    void updateProductRejectsUnknownCategory() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product()));
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> productService.updateProduct(productId,
                new UpdateProductRequest("Keyboard", null, BigDecimal.TEN, categoryId)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changeStatusPublishesOnlyWhenStatusChanges() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product()));

        productService.changeStatus(productId, ProductStatus.ACTIVE);
        verify(eventPublisher, never()).publish(anyString(), any());

        productService.changeStatus(productId, ProductStatus.INACTIVE);
        verify(eventPublisher).publish(eq("ProductStatusChanged"), any(Product.class));
    }

    @Test
    void getProductFailsForUnknownId() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(productId)).isInstanceOf(NotFoundException.class);
    }

    private static Product product() {
        return new Product("SKU-1", "Keyboard", null, BigDecimal.TEN, null, ProductStatus.ACTIVE);
    }
}
