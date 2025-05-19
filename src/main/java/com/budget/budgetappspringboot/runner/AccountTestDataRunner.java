package com.budget.budgetappspringboot.runner;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.model.enums.AccountType;
import com.budget.budgetappspringboot.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order; // For controlling execution order
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@Order(2) // Specifies execution order; runs after CategoryTestDataRunner
public class AccountTestDataRunner implements CommandLineRunner {

    private final AccountRepository accountRepository;

    public AccountTestDataRunner(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("--- [AccountTestDataRunner]: Setting up test data for Accounts ---");

        // Using the constructor: Account(String name, AccountType accountType, BigDecimal initialBalance)
        List<Account> accountsToSeed = Arrays.asList(
                new Account("Main Checking", AccountType.CHECKING, new BigDecimal("1500.75")),
                new Account("Visa Rewards Card", AccountType.CREDIT_CARD, new BigDecimal("-350.20")),
                new Account("Main Savings", AccountType.SAVINGS, new BigDecimal("5250.00")),
                new Account("Cash Wallet", AccountType.CASH, new BigDecimal("120.00")),
                new Account("Investment", AccountType.INVESTMENT, new BigDecimal( "2000.0")),
                new Account("Loan", AccountType.LOAN, new BigDecimal( "160000.0")),
                new Account("Brokerage", AccountType.BROKERAGE, new BigDecimal( "6000.0")),
                new Account("Cash Management", AccountType.CASH_MANAGEMENT_BROKERAGE, new BigDecimal( "500.0")),
                new Account("Other", AccountType.OTHER, new BigDecimal( "1500.0"))

/*
                CHECKING("Checking Account"),
                CREDIT_CARD("Credit Card"),
                SAVINGS("Savings Account"),
                CASH("Cash"),
                INVESTMENT("Investment Account"),
                LOAN("Loan"),
                BROKERAGE("Brokerage Account"),
                CASH_MANAGEMENT_BROKERAGE("Cash Management Brokerage Account"),
                OTHER("Other");
*/
        );

        for (Account acc : accountsToSeed) {
            Optional<Account> existingAccount = accountRepository.findByName(acc.getName());
            if (existingAccount.isEmpty()) {
                accountRepository.save(acc);
                log.info("Saved account: Name='{}', Type='{}', Balance='{}'", acc.getName(), acc.getAccountType().getDisplayName(), acc.getBalance());
            } else {
                log.info("Account with name '{}' already exists. Skipping.", acc.getName());
            }
        }

        log.info("--- [AccountTestDataRunner]: Retrieving all accounts ---");
        List<Account> allAccounts = accountRepository.findAll();
        if (allAccounts.isEmpty()) {
            log.info("No accounts found in the database.");
        } else {
            allAccounts.forEach(account ->
                    log.info("Account - ID: {}, Name: '{}', Type: '{}', Balance: {}",
                            account.getId(), account.getName(), account.getAccountType().getDisplayName(), account.getBalance())
            );
        }
        log.info("--- [AccountTestDataRunner]: Finished ---");
    }
}