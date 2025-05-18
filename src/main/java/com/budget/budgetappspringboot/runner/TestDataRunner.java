package com.budget.budgetappspringboot.runner;

import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

@Component
@Slf4j
public class TestDataRunner implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public TestDataRunner(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("--- CommandLineRunner (com.budget.budgetappspringboot): Setting up test data for Categories ---");

        //for testing - clean out existing categories
        //categoryRepository.deleteAll();
        //log.info("All existing categories deleted via CommandLineRunner.");

        List<String> categoryNames = Arrays.asList("Groceries", "Utilities", "Restaurants", "Shopping", "Entertainment", "Automotive", "Healthcare", "Education", "Travel");

        for (String name : categoryNames) {
            Optional<Category> existingCategory = categoryRepository.findByName(name);
            if (existingCategory.isEmpty()) {
                Category newCategory = new Category(null, name);
                categoryRepository.save(newCategory);
                log.info("Saved category: {}", newCategory.getName());
            } else {
                log.info("Category {} already exists. Skipping.", name);
            }
        }

        log.info("--- Retrieving all categories ---");
        List<Category> allCategories = categoryRepository.findAll();
        if (allCategories.isEmpty()) {
            log.info("No categories found in the database.");
        } else {
            allCategories.forEach(category -> log.info("Category - ID: {}, Name: {}", category.getId(), category.getName()));
        }

        log.info("--- Testing findByName for 'Utilities' ---");
        Optional<Category> foundUtilities = categoryRepository.findByName("Utilities");
        if(foundUtilities.isPresent()) {
            log.info("Found 'Utilities': {}", foundUtilities.get());
        } else {
            log.warn("'Utilities' category not found by name.");
        }

        log.info("--- Testing findByNameIgnoreCase for 'entertainent' ---");
        Optional<Category> foundEntertainmentIgnoreCase = categoryRepository.findByNameIgnoreCase("entertainment");
        if (foundEntertainmentIgnoreCase.isPresent()) {
            log.info("Found 'entertainment' (ignore case): {}", foundEntertainmentIgnoreCase.get());
        } else {
            log.warn("'entertainment' category (ingore case) not found by name.");
        }

        log.info("--- CommandLineRunner (com.budget.budgetappspringboot): Test data setup finished ---");
    }
}