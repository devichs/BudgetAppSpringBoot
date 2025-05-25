package com.budget.budgetappspringboot.service.impl;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.entity.Transaction;
import com.budget.budgetappspringboot.model.enums.TransactionType;
import com.budget.budgetappspringboot.repository.AccountRepository;
import com.budget.budgetappspringboot.repository.CategoryRepository;
import com.budget.budgetappspringboot.repository.TransactionRepository;
import com.budget.budgetappspringboot.service.TransactionService;
import jakarta.persistence.EntityNotFoundException; // Or a custom exception
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountRepository accountRepository,
                                  CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Transaction createTransaction(BigDecimal amount, TransactionType transactionType,
                                         LocalDate transactionDate, String description,
                                         Long categoryId, Long accountId) {

        log.info("Creating transaction: Amount={}, Type={}, Date={}, Desc='{}', CategoryID={}, AccountID={}",
                amount, transactionType, transactionDate, description, categoryId, accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.error("Account not found with ID: {}", accountId);
                    return new EntityNotFoundException("Account not found with ID: " + accountId);
                });

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> {
                        log.error("Category not found with ID: {}", categoryId);
                        return new EntityNotFoundException("Category not found with ID: " + categoryId);
                    });
        }


        Transaction transaction = new Transaction(amount, transactionType, transactionDate,
                description, category, account);
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved with ID: {}", savedTransaction.getId());


        if (transactionType == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
            log.info("Income transaction. New balance for account {}: {}", account.getName(), account.getBalance());
        } else if (transactionType == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(amount));
            log.info("Expense transaction. New balance for account {}: {}", account.getName(), account.getBalance());
        }

        accountRepository.save(account);
        log.info("Account {} balance updated.", account.getName());

        return savedTransaction;
    }

    @Override
    @Transactional
    public List<Transaction> getAllTransactions() {
        log.info("Fetching all transactions");
        return transactionRepository.findAll();
    }

    @Override
    @Transactional
    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with ID: " + accountId));
        log.info("Fetching transactions for account ID: {}", accountId);

        return transactionRepository.findByAccount(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByCategoryAndDateRange(Long categoryId, LocalDate startDate, LocalDate endDate) {
        if (categoryId == null || startDate == null || endDate == null) {
            log.warn("Category ID, start date, or end date cannot be null for filtering transactions.");
            return List.of();
        }
        if (startDate.isAfter(endDate)) {
            log.warn("Start date cannot be after end date for filtering transactions.");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", categoryId);
                    return new EntityNotFoundException("Category not found with ID: " + categoryId);
                });

        log.info("Fetching transactions for category '{}' between {} and {}", category.getName(), startDate, endDate);
        return transactionRepository.findByCategoryAndTransactionDateBetweenWithDetails(category, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            log.warn("Start date or end date cannot be null for filtering all transactions by date range.");
            return List.of();
        }
        if (startDate.isAfter(endDate)) {
            log.warn("Start date cannot be after end date for filtering transactions.");
            return List.of();
        }
        log.info("Fetching all transactions between {} and {}", startDate, endDate);
        return transactionRepository.findAllByTransactionDateBetweenWithDetails(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByTypeAndDateRange(TransactionType transactionType, LocalDate startDate, LocalDate endDate) {
        if (transactionType == null || startDate == null || endDate == null) {
            log.warn("Transaction type, start date, or end date cannot be null for filtering.");
            return List.of();
        }
        if (startDate.isAfter(endDate)) {
            log.warn("Start date cannot be after end date for filtering transactions.");
            return List.of();
        }

        log.info("Fetching transactions of type '{}' between {} and {}", transactionType.getDisplayName(), startDate, endDate);
        return transactionRepository.findByTransactionTypeAndTransactionDateBetweenWithDetails(transactionType, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> searchTransactionsByDescription(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("Search keyword cannot be null or empty.");
            // Or throw new IllegalArgumentException("Search keyword is required.");
            return List.of(); // Return empty list if keyword is invalid
        }
        log.info("Searching transactions with description containing: '{}'", keyword);
        return transactionRepository.findByDescriptionContainingIgnoreCaseWithDetails(keyword.trim());
    }

}