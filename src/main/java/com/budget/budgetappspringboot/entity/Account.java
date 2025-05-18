package com.budget.budgetappspringboot.entity;

import com.budget.budgetappspringboot.model.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts")

public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    public Account(String name, AccountType accountType) {
        this.name = name;
        this.accountType = accountType;
        this.balance = BigDecimal.ZERO;
    }

    public Account(String name, AccountType accountType, BigDecimal initialBalance) {
        this.name = name;
        this.accountType = accountType;
        this.balance = (initialBalance != null) ? initialBalance : BigDecimal.ZERO;
    }
}
