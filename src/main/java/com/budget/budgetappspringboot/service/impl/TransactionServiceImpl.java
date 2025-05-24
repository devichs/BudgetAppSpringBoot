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

@Service // Marks this class as a Spring service component
@Slf4j   // For logging
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    // Constructor-based dependency injection
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountRepository accountRepository,
                                  CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional // Crucial: Ensures all operations are part of a single database transaction
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

        Category category = null; // Category can be optional for some transactions
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> {
                        log.error("Category not found with ID: {}", categoryId);
                        return new EntityNotFoundException("Category not found with ID: " + categoryId);
                    });
        }

        // Create and save the transaction
        Transaction transaction = new Transaction(amount, transactionType, transactionDate,
                description, category, account);
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved with ID: {}", savedTransaction.getId());

        // Update account balance
        if (transactionType == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
            log.info("Income transaction. New balance for account {}: {}", account.getName(), account.getBalance());
        } else if (transactionType == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().subtract(amount));
            log.info("Expense transaction. New balance for account {}: {}", account.getName(), account.getBalance());
        }

        accountRepository.save(account); // Save the updated account
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
        // Assuming TransactionRepository has a method like: List<Transaction> findByAccount(Account account);
        return transactionRepository.findByAccount(account);
    }
}