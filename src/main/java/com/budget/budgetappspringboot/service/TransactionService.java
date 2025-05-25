package com.budget.budgetappspringboot.service;

import com.budget.budgetappspringboot.entity.Transaction;
import com.budget.budgetappspringboot.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    Transaction createTransaction(BigDecimal amount, TransactionType transactionType,
                                  LocalDate transactionDate, String description,
                                  Long categoryId, Long accountId);

    List<Transaction> getAllTransactions();

    List<Transaction> getTransactionsByAccountId(Long accountId);

    List<Transaction> getTransactionsByCategoryAndDateRange(Long categoryId, LocalDate startDate, LocalDate endDate);

    List<Transaction> getAllTransactionsByDateRange(LocalDate startDate, LocalDate endDate);

    List<Transaction> getTransactionsByTypeAndDateRange(TransactionType transactionType, LocalDate startDate, LocalDate endDate);

    List<Transaction> searchTransactionsByDescription(String keyword);
}