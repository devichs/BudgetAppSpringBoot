package com.budget.budgetappspringboot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table (name = "budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"category_id", "budget_year" , "budget_month"})
})

public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "category_id", nullable = false)
    private Category category;

    @Column (name = "budget_year", nullable = false)
    private int year;

    @Column (name = "budget_month", nullable = false)
    private int month;

    @Column (name = "budgeted_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal budgetedAmount;

    @CreationTimestamp
    @Column (name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column (name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;

    public Budget(Category category, int year, int month, BigDecimal budgetedAmount) {
        this.category = category;
        this.year = year;
        this.month = month;
        this.budgetedAmount = budgetedAmount;
    }
}
