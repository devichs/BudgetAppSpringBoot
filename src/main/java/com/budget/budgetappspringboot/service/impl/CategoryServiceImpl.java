package com.budget.budgetappspringboot.service.impl;

import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.repository.CategoryRepository;
import com.budget.budgetappspringboot.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        log.info("Fetching all categories");
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findCategoryById(Long id) {
        log.info("Fetching category by ID: {}", id);
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findCategoryByName(String name) {
        log.info("Fetching category by name: {}", name);
        return categoryRepository.findByName(name);
    }

    @Override
    @Transactional
    public Category findOrCreateCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            log.warn("Category name is null or empty, cannot find or create a category");
            return null;
        }
        Optional<Category> existingCategory = categoryRepository.findByNameIgnoreCase(categoryName.trim());
        if (existingCategory.isPresent()) {
            log.info("Found existing category: {}", categoryName);
            return existingCategory.get();
        } else {
            log.info("Category '{}' not found, creating a new one.", categoryName);
            Category newCategory = new Category(null, categoryName.trim());
            return categoryRepository.save(newCategory);
        }
    }
}