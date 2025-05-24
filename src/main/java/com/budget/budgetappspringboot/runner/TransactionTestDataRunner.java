package com.budget.budgetappspringboot.runner;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.entity.Transaction; // Make sure Transaction is imported
import com.budget.budgetappspringboot.model.enums.TransactionType;
import com.budget.budgetappspringboot.repository.AccountRepository;
import com.budget.budgetappspringboot.repository.CategoryRepository;
import com.budget.budgetappspringboot.repository.TransactionRepository; // To list transactions at the end
import com.budget.budgetappspringboot.service.TransactionService;   // <<< Import your new service
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
// No @Transactional needed here, as the service method should be transactional

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;

@Component
@Slf4j
@Order(3) // Ensure this runs after CategoryTestDataRunner and AccountTestDataRunner
public class TransactionTestDataRunner implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService; // <<< Inject TransactionService
    private final TransactionRepository transactionRepository; // <<< Keep for listing all transactions

    // Updated constructor to inject TransactionService
    public TransactionTestDataRunner(CategoryRepository categoryRepository,
                                     AccountRepository accountRepository,
                                     TransactionService transactionService,
                                     TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("--- [TransactionTestDataRunner]: Setting up transactions using TransactionService ---");

        // Fetch prerequisite Categories and Accounts
        // These should have been created by CategoryTestDataRunner and AccountTestDataRunner
        Category catSalary = categoryRepository.findByName("Salary")
                .orElseThrow(() -> new NoSuchElementException("Category 'Salary' not found. Ensure prerequisite runners have executed."));
        Category catGroceries = categoryRepository.findByName("Groceries")
                .orElseThrow(() -> new NoSuchElementException("Category 'Groceries' not found."));
        Category catEntertainment = categoryRepository.findByName("Entertainment")
                .orElseThrow(() -> new NoSuchElementException("Category 'Entertainment' not found."));

        Account checkingAccount = accountRepository.findByName("Main Checking")
                .orElseThrow(() -> new NoSuchElementException("Account 'Main Checking' not found. Ensure prerequisite runners have executed."));
        Account creditCard = accountRepository.findByName("Visa Rewards Card")
                .orElseThrow(() -> new NoSuchElementException("Account 'Visa Rewards Card' not found."));

        // Log initial balances for verification
        log.info("Initial balance for '{}': {}", checkingAccount.getName(), checkingAccount.getBalance());
        log.info("Initial balance for '{}': {}", creditCard.getName(), creditCard.getBalance());

        // --- Create transactions using the TransactionService ---
        try {
            log.info("Attempting to create Salary transaction...");
            transactionService.createTransaction(
                    new BigDecimal("3500.00"), TransactionType.INCOME, LocalDate.now().withDayOfMonth(1),
                    "Monthly Salary", catSalary.getId(), checkingAccount.getId());
            log.info("Salary transaction created successfully via service.");

            log.info("Attempting to create Groceries transaction...");
            transactionService.createTransaction(
                    new BigDecimal("125.50"), TransactionType.EXPENSE, LocalDate.now().minusDays(5),
                    "Weekly Groceries", catGroceries.getId(), checkingAccount.getId());
            log.info("Groceries transaction created successfully via service.");

            log.info("Attempting to create Entertainment transaction...");
            transactionService.createTransaction(
                    new BigDecimal("45.00"), TransactionType.EXPENSE, LocalDate.now().minusDays(2),
                    "Movie Tickets", catEntertainment.getId(), creditCard.getId());
            log.info("Entertainment transaction created successfully via service.");

        } catch (Exception e) {
            log.error("!!! Error during transaction creation via service: {}", e.getMessage(), e);
        }

        // --- Verify account balance updates ---
        log.info("--- Verifying Account Balances After Transactions ---");

        Account updatedCheckingAccount = accountRepository.findById(checkingAccount.getId())
                .orElseThrow(() -> new NoSuchElementException("Checking account (ID: " + checkingAccount.getId() + ") vanished after transactions!"));
        Account updatedCreditCard = accountRepository.findById(creditCard.getId())
                .orElseThrow(() -> new NoSuchElementException("Credit card (ID: " + creditCard.getId() + ") vanished after transactions!"));

        log.info("Updated balance for '{}' (ID: {}): {}", updatedCheckingAccount.getName(), updatedCheckingAccount.getId(), updatedCheckingAccount.getBalance());
        log.info("Updated balance for '{}' (ID: {}): {}", updatedCreditCard.getName(), updatedCreditCard.getId(), updatedCreditCard.getBalance());

        // --- List all transactions from the database ---
        log.info("--- [TransactionTestDataRunner]: Retrieving all transactions from repository ---");
        transactionRepository.findAll().forEach(transaction ->
                log.info("Transaction - ID: {}, Date: {}, Type: {}, Amount: {}, Desc: '{}', Account: '{}', Category: '{}'",
                        transaction.getId(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionType().getDisplayName(),
                        transaction.getAmount(),
                        transaction.getDescription(),
                        transaction.getAccount().getName(), // Assumes Account has getName()
                        transaction.getCategory() != null ? transaction.getCategory().getName() : "N/A" // Assumes Category has getName()
                )
        );
        log.info("--- [TransactionTestDataRunner]: Finished ---");
    }
}