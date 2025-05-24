package com.budget.budgetappspringboot.service;

import com.budget.budgetappspringboot.dto.BudgetStatusDTO;
import com.budget.budgetappspringboot.entity.Budget;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BudgetService {
    Budget setBudget(Long categoryId, int year, int month,BigDecimal amount);
    Optional<Budget> getBudget(Long categoryId, int year, int month);
    List<Budget> getBudgetsForPeriod(int year, int month);
    BigDecimal getActualSpending(Long categoryId, int year, int month);
    Optional<BudgetStatusDTO> getBudgetStatusForCategory(Long categoryId, int year, int month);
    List<BudgetStatusDTO>getOverallBudgetStatusForPeriod(int year, int month);
}
