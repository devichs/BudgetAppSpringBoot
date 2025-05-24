package com.budget.budgetappspringboot.service.impl;

import com.budget.budgetappspringboot.dto.BudgetStatusDTO;
import com.budget.budgetappspringboot.entity.Budget;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.entity.Transaction;
import com.budget.budgetappspringboot.model.enums.TransactionType;
import com.budget.budgetappspringboot.repository.BudgetRepository;
import com.budget.budgetappspringboot.repository.CategoryRepository;
import com.budget.budgetappspringboot.repository.TransactionRepository;
import com.budget.budgetappspringboot.service.BudgetService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public BudgetServiceImpl(BudgetRepository budgetRepository,
                             CategoryRepository categoryRepository,
                             TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Budget setBudget(Long categoryId, int year, int month, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Budgeted amount cannot be negative.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + categoryId));

        // Validate month
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }

        Optional<Budget> existingBudgetOpt = budgetRepository.findByCategoryAndYearAndMonth(category, year, month);

        Budget budget;
        if (existingBudgetOpt.isPresent()) {
            budget = existingBudgetOpt.get();
            log.info("Updating existing budget for Category '{}', Period: {}-{}, Amount: {}",
                    category.getName(), year, month, amount);
            budget.setBudgetedAmount(amount);
        } else {
            log.info("Creating new budget for Category '{}', Period: {}-{}, Amount: {}",
                    category.getName(), year, month, amount);
            budget = new Budget(category, year, month, amount);
        }
        return budgetRepository.save(budget);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Budget> getBudget(Long categoryId, int year, int month) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + categoryId));
        return budgetRepository.findByCategoryAndYearAndMonth(category, year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsForPeriod(int year, int month) {
        // Validate month
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
        return budgetRepository.findByYearAndMonth(year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getActualSpending(Long categoryId, int year, int month) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + categoryId));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByCategoryAndTransactionDateBetween(
                category, startDate, endDate);

        return transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BudgetStatusDTO> getBudgetStatusForCategory(Long categoryId, int year, int month) {
        Optional<Budget> budgetOpt = getBudget(categoryId, year, month);
        if (budgetOpt.isEmpty()) {
            return Optional.empty(); // No budget set for this category/period
        }
        Budget budget = budgetOpt.get();
        BigDecimal actualSpending = getActualSpending(categoryId, year, month);
        BigDecimal remaining = budget.getBudgetedAmount().subtract(actualSpending);

        return Optional.of(new BudgetStatusDTO(
                budget.getCategory().getName(),
                budget.getCategory().getId(),
                year,
                month,
                budget.getBudgetedAmount(),
                actualSpending,
                remaining
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetStatusDTO> getOverallBudgetStatusForPeriod(int year, int month) {
        List<Budget> budgetsForPeriod = getBudgetsForPeriod(year, month);
        return budgetsForPeriod.stream()
                .map(budget -> {
                    BigDecimal actualSpending = getActualSpending(budget.getCategory().getId(), year, month);
                    BigDecimal remaining = budget.getBudgetedAmount().subtract(actualSpending);
                    return new BudgetStatusDTO(
                            budget.getCategory().getName(),
                            budget.getCategory().getId(),
                            year,
                            month,
                            budget.getBudgetedAmount(),
                            actualSpending,
                            remaining
                    );
                })
                .collect(Collectors.toList());
    }
}