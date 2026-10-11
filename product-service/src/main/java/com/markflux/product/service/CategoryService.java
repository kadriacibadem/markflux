package com.markflux.product.service;

import com.markflux.common.error.ConflictException;
import com.markflux.product.domain.Category;
import com.markflux.product.repository.CategoryRepository;
import com.markflux.product.web.dto.CategoryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(String name) {
        String trimmedName = name.trim();
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("CATEGORY_ALREADY_EXISTS", "Category already exists: " + trimmedName);
        }
        return CategoryResponse.from(categoryRepository.save(new Category(trimmedName)));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }
}
