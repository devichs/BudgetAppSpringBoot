package com.budget.budgetappspringboot.runner; // Or your package if different

import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j; // Lombok: For easy logging with SLF4J
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Good practice for DB operations

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component // Marks this class as a Spring component, so Spring Boot will discover and run it
@Slf4j     // Lombok: Automatically creates an SLF4J logger instance named 'log'
public class TestDataRunner implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    // Spring Boot automatically injects dependencies through the constructor (Constructor Injection)
    // @Autowired annotation on constructor is optional if there's only one constructor
    public TestDataRunner(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional // Ensures all database operations in this method are part of a single transaction
    // (Good practice, though not strictly essential for this simple example)
    public void run(String... args) throws Exception {
        log.info("--- CommandLineRunner: Setting up test data for Categories ---");

        // Optional: Clean up existing categories if you want to start fresh each time
        // categoryRepository.deleteAll();
        // log.info("All existing categories deleted.");

        // Create some new Category objects
        Category catGroceries = new Category(null, "Groceries"); // id is null as it's auto-generated
        Category catUtilities = new Category(null, "Utilities");
        Category catEntertainment = new Category(null, "Entertainment");
        Category catTransport = new Category(null, "Transport");

        // Check if "Groceries" already exists to avoid unique constraint violation if re-running
        Optional<Category> existingGroceries = categoryRepository.findByName("Groceries");
        if (existingGroceries.isEmpty()) {
            categoryRepository.save(catGroceries);
            log.info("Saved category: {}", catGroceries.getName());
        } else {
            log.info("Category 'Groceries' already exists.");
        }

        Optional<Category> existingUtilities = categoryRepository.findByName("Utilities");
        if (existingUtilities.isEmpty()) {
            categoryRepository.save(catUtilities);
            log.info("Saved category: {}", catUtilities.getName());
        } else {
            log.info("Category 'Utilities' already exists.");
        }

        // Using saveAll for the rest for brevity if they don't exist
        // (A more robust check would be needed for each if you run this multiple times without deleteAll)
        if (categoryRepository.findByName("Entertainment").isEmpty()) {
            categoryRepository.save(catEntertainment);
            log.info("Saved category: Entertainment");
        }
        if (categoryRepository.findByName("Transport").isEmpty()) {
            categoryRepository.save(catTransport);
            log.info("Saved category: Transport");
        }


        log.info("--- Retrieving all categories ---");
        List<Category> allCategories = categoryRepository.findAll();
        if (allCategories.isEmpty()) {
            log.info("No categories found in the database.");
        } else {
            allCategories.forEach(category ->
                    log.info("Category - ID: {}, Name: {}", category.getId(), category.getName())
            );
        }

        log.info("--- Testing findByName for 'Utilities' ---");
        Optional<Category> foundUtilities = categoryRepository.findByName("Utilities");
        if (foundUtilities.isPresent()) {
            log.info("Found 'Utilities': {}", foundUtilities.get());
        } else {
            log.warn("'Utilities' category not found by name.");
        }

        log.info("--- Testing findByNameIgnoreCase for 'entertainment' ---");
        Optional<Category> foundEntertainmentIgnoreCase = categoryRepository.findByNameIgnoreCase("entertainment");
        if (foundEntertainmentIgnoreCase.isPresent()) {
            log.info("Found 'entertainment' (ignore case): {}", foundEntertainmentIgnoreCase.get());
        } else {
            log.warn("'entertainment' category (ignore case) not found by name.");
        }

        log.info("--- CommandLineRunner: Test data setup finished ---");
    }
}