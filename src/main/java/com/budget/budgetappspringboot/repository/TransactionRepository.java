package com.budget.budgetappspringboot.repository;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.entity.Transaction;
// ... other imports ...
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Old method - might cause LazyInitializationException if Category/Account accessed outside session
    // List<Transaction> findByAccount(Account account);

    // New method with JOIN FETCH to eagerly load Account and Category
    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category WHERE t.account = :account")
    List<Transaction> findByAccountWithDetails(@Param("account") Account account);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category WHERE t.account = :account")
    List<Transaction> findByAccount(Account account);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.account LEFT JOIN FETCH t.category")
    List<Transaction> findAllWithDetails();


    // ... other custom finder methods ...
}