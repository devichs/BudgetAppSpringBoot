package com.budget.budgetappspringboot.repository;

import com.budget.budgetappspringboot.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository {
    Optional<Account> findByName(String name);
    Optional<Account> findByNameIgnoreCase(String name);
}
