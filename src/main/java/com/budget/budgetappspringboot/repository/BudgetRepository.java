package com.budget.budgetappspringboot.repository;

import com.budget.budgetappspringboot.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import com.budget.budgetappspringboot.entity.Category;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByCategoryAndYearAndMonth(Category category, Integer year, Integer month);
    List<Budget> findByYearAndMonth(Integer year, Integer month);
    List<Budget> findByCategory(Category category);
}
