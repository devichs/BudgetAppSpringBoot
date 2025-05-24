package com.budget.budgetappspringboot.service;

import com.budget.budgetappspringboot.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountService {

    List<Account> getAllAccounts();

    Optional<Account> findAccountById(Long id);

    Optional<Account> findAccountByName(String name);

    // We can add methods like createAccount, updateAccount, deleteAccount later if needed
    // For now, TransactionService updates balances directly.
}