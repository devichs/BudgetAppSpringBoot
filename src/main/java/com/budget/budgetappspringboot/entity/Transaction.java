package com.budget.budgetappspringboot.entity;

import com.budget.budgetappspringboot.model.enums.TransactionType; // Your enum
import jakarta.persistence.*; // For JPA annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2) // For monetary values
    private BigDecimal amount; // Should represent the absolute value; type determines effect

    @Enumerated(EnumType.STRING) // Store enum as its String name
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(length = 255) // Optional description
    private String description;

    @ManyToOne(fetch = FetchType.LAZY) // Many transactions to one category
    @JoinColumn(name = "category_id", nullable = true) // Foreign key. Nullable if a transaction might not have a category.
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY) // Many transactions to one account
    @JoinColumn(name = "account_id", nullable = false) // Foreign key. A transaction must belong to an account.
    private Account account;

    @CreationTimestamp // Hibernate: Automatically set on entity creation
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp // Hibernate: Automatically set on entity update
    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;

    // Custom constructor for convenience (ID and audit dates are auto-generated/managed)
    public Transaction(BigDecimal amount, TransactionType transactionType, LocalDate transactionDate,
                       String description, Category category, Account account) {
        this.amount = amount;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.description = description;
        this.category = category;
        this.account = account;
    }
}