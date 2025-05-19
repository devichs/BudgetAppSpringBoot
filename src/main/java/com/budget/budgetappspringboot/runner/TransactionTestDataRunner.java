package com.budget.budgetappspringboot.runner;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.entity.Transaction;
import com.budget.budgetappspringboot.model.enums.AccountType; // Your AccountType enum
import com.budget.budgetappspringboot.model.enums.TransactionType; // Your TransactionType enum
import com.budget.budgetappspringboot.repository.AccountRepository;
import com.budget.budgetappspringboot.repository.CategoryRepository;
import com.budget.budgetappspringboot.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException; // For orElseThrow

@Component
@Slf4j
@Order(3) // Ensure this runs after CategoryTestDataRunner and AccountTestDataRunner
public class TransactionTestDataRunner implements CommandLineRunner {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public TransactionTestDataRunner(TransactionRepository transactionRepository,
                                     CategoryRepository categoryRepository,
                                     AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("--- [TransactionTestDataRunner]: Setting up test data for Transactions ---");

        // Ensure prerequisite runners have run and data exists
        // Fetch some Categories (assuming they were created by CategoryTestDataRunner)
        Category catSalary = categoryRepository.findByName("Salary")
                .orElseThrow(() -> new NoSuchElementException("Category 'Salary' not found. Ensure CategoryTestDataRunner has run."));
        Category catGroceries = categoryRepository.findByName("Groceries")
                .orElseThrow(() -> new NoSuchElementException("Category 'Groceries' not found."));
        Category catUtilities = categoryRepository.findByName("Utilities")
                .orElseThrow(() -> new NoSuchElementException("Category 'Utilities' not found."));
        Category catEntertainment = categoryRepository.findByName("Entertainment")
                .orElseThrow(() -> new NoSuchElementException("Category 'Entertainment' not found."));
        Category catDiningOut = categoryRepository.findByName("Restaurants")
                .orElseThrow(() -> new NoSuchElementException("Category 'Restaurants' not found."));


        // Fetch some Accounts (assuming they were created by AccountTestDataRunner)
        // Using your specific AccountType names: CHECKING, CREDIT_CARD
        Account checkingAccount = accountRepository.findByName("Main Checking") // Assuming this account name exists
                .orElseThrow(() -> new NoSuchElementException("Account 'Main Checking' not found. Ensure AccountTestDataRunner has run."));
        Account creditCard = accountRepository.findByName("Visa Rewards Card") // Assuming this account name exists
                .orElseThrow(() -> new NoSuchElementException("Account 'Visa Rewards Card' not found."));


        // Create some transactions
        // Transaction(BigDecimal amount, TransactionType transactionType, LocalDate transactionDate,
        //             String description, Category category, Account account)

        List<Transaction> transactionsToSeed = Arrays.asList(
                new Transaction(new BigDecimal("3500.00"), TransactionType.INCOME, LocalDate.now().withDayOfMonth(1),
                        "Monthly Salary", catSalary, checkingAccount),
                new Transaction(new BigDecimal("125.50"), TransactionType.EXPENSE, LocalDate.now().minusDays(5),
                        "Weekly Groceries", catGroceries, checkingAccount),
                new Transaction(new BigDecimal("85.00"), TransactionType.EXPENSE, LocalDate.now().minusDays(3),
                        "Electricity Bill", catUtilities, checkingAccount),
                new Transaction(new BigDecimal("45.00"), TransactionType.EXPENSE, LocalDate.now().minusDays(2),
                        "Movie Tickets", catEntertainment, creditCard),
                new Transaction(new BigDecimal("60.75"), TransactionType.EXPENSE, LocalDate.now().minusDays(1),
                        "Dinner with friends", catDiningOut, creditCard),
                new Transaction(new BigDecimal("22.00"), TransactionType.EXPENSE, LocalDate.now().minusDays(4),
                        "Coffee and Snacks", catDiningOut, checkingAccount)
        );

        // Note: This basic seeding does NOT update account balances.
        // Account balance updates should ideally be handled by a service layer
        // when actual transactions are created in a real application.
        // For test data, we are just creating the transaction records.

        transactionRepository.saveAll(transactionsToSeed);
        log.info("Saved {} initial transactions.", transactionsToSeed.size());

        log.info("--- [TransactionTestDataRunner]: Retrieving all transactions ---");
        List<Transaction> allTransactions = transactionRepository.findAll();
        if (allTransactions.isEmpty()) {
            log.info("No transactions found in the database.");
        } else {
            allTransactions.forEach(transaction ->
                    log.info("Transaction - ID: {}, Date: {}, Type: {}, Amount: {}, Desc: '{}', Account: '{}', Category: '{}'",
                            transaction.getId(),
                            transaction.getTransactionDate(),
                            transaction.getTransactionType().getDisplayName(),
                            transaction.getAmount(),
                            transaction.getDescription(),
                            transaction.getAccount().getName(), // Assuming Account has getName()
                            transaction.getCategory() != null ? transaction.getCategory().getName() : "N/A" // Assuming Category has getName()
                    )
            );
        }
        log.info("--- [TransactionTestDataRunner]: Finished ---");
    }
}