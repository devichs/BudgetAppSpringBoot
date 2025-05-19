package com.budget.budgetappspringboot.repository;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.entity.Transaction;
import com.budget.budgetappspringboot.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Spring Data JPA will automatically generate queries based on method names.
    // Examples:
    List<Transaction> findByAccount(Account account);
    List<Transaction> findByOrderByTransactionDateDesc();
    List<Transaction> findByCategory(Category category);
    List<Transaction> findByTransactionType(TransactionType transactionType);
    List<Transaction> findByTransactionDate(LocalDate transactionDate);
    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    List<Transaction> findByAccountAndTransactionDateBetween(Account account, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByCategoryAndTransactionDateBetween(Category category, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByDescriptionContainingIgnoreCase(String keyword);
}