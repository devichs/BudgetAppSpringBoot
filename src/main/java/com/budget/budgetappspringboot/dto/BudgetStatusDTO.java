package com.budget.budgetappspringboot.dto;

import java.math.BigDecimal;

public record BudgetStatusDTO(
        String categoryName,
        Long categoryId,
        int year,
        int month,
        BigDecimal budgetedAmount,
        BigDecimal actualSpending,
        BigDecimal remainingOrOverspent
) {}
