package com.budget.budgetappspringboot.repository;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.model.enums.TransactionType;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category WHERE t.account = :account")
    List<Transaction> findByAccountWithDetails(@Param("account") Account account);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category WHERE t.account = :account")
    List<Transaction> findByAccount(Account account);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category")
    List<Transaction> findAllWithDetails();

    List<Transaction> findByCategoryIdAndTransactionDateBetween(Long categoryId, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByOrderByTransactionDateDesc();
    List<Transaction> findByTransactionType(TransactionType transactionType);
    List<Transaction> findByDescriptionContainingIgnoreCase(String keyword);
    List<Transaction> findByTransactionDate(LocalDate transactionDate);
    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    List<Transaction> findByAccountAndTransactionDateBetween(Account account, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a LEFT JOIN FETCH t.category c " +
            "WHERE c = :category AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate ASC")
    List<Transaction> findByCategoryAndTransactionDateBetweenWithDetails(
            @Param("category") Category category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a LEFT JOIN FETCH t.category c " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate ASC, t.id ASC")
    List<Transaction> findAllByTransactionDateBetweenWithDetails(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a LEFT JOIN FETCH t.category c " +
            "WHERE t.transactionType = :transactionType AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate ASC, t.id ASC")
    List<Transaction> findByTransactionTypeAndTransactionDateBetweenWithDetails(
            @Param("transactionType") TransactionType transactionType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a LEFT JOIN FETCH t.category c " +
            "WHERE LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findByDescriptionContainingIgnoreCaseWithDetails(@Param("keyword") String keyword);

    List<Transaction> findByCategoryAndTransactionDateBetween(Category category, LocalDate startDate, LocalDate endDate);
}