package com.budget.budgetappspringboot.model.enums;

public enum AccountType {
    CHECKING("Checking Account"),
    CREDIT_CARD("Credit Card"),
    SAVINGS("Savings Account"),
    CASH("Cash"),
    INVESTMENT("Investment Account"),
    LOAN("Loan"),
    BROKERAGE("Brokerage Account"),
    CASH_MANAGEMENT_BROKERAGE("Cash Management Brokerage Account"),
    OTHER("Other");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
