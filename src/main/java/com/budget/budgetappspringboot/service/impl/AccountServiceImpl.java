package com.budget.budgetappspringboot.service.impl;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.repository.AccountRepository;
import com.budget.budgetappspringboot.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Good for read-only methods too

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true) // Good practice for read-only operations
    public List<Account> getAllAccounts() {
        log.info("Fetching all accounts");
        return accountRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findAccountById(Long id) {
        log.info("Fetching account by ID: {}", id);
        return accountRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findAccountByName(String name) {
        log.info("Fetching account by name: {}", name);
        return accountRepository.findByName(name);
    }
}