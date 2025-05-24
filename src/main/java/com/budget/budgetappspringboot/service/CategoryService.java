package com.budget.budgetappspringboot.service;

import com.budget.budgetappspringboot.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> getAllCategories();
    Optional<Category> findCategoryById(Long id);
    Optional<Category> findCategoryByName(String name);
    // Add createCategory later if needed from CLI
}