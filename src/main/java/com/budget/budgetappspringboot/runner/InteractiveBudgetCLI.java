package com.budget.budgetappspringboot.runner;

import com.budget.budgetappspringboot.dto.BudgetStatusDTO;
import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.entity.Transaction;
import com.budget.budgetappspringboot.model.enums.TransactionType;
import com.budget.budgetappspringboot.service.AccountService;
import com.budget.budgetappspringboot.service.BudgetService;
import com.budget.budgetappspringboot.service.CategoryService;
import com.budget.budgetappspringboot.service.TransactionService;
import com.budget.budgetappspringboot.service.impl.AccountServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.budget.budgetappspringboot.dto.ImportSummary;
import com.budget.budgetappspringboot.service.CsvImportService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Component
@Slf4j
@Order(10)
public class InteractiveBudgetCLI implements CommandLineRunner {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final CsvImportService csvImportService;
    private final BudgetService budgetService;
    private final Scanner scanner;

    public InteractiveBudgetCLI(TransactionService transactionService,
                                AccountService accountService,
                                CategoryService categoryService,
                                CsvImportService csvImportService,
                                BudgetService budgetService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.csvImportService = csvImportService;
        this.budgetService = budgetService;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Welcome to the Interactive Budget CLI!");

        boolean running = true;
        while (running) {
            displayMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleAddTransaction(TransactionType.INCOME);
                    break;
                case "2":
                    handleAddTransaction(TransactionType.EXPENSE);
                    break;
                case "3":
                    handleViewAccountTransactions();
                    break;
                case "4":
                    handleViewAccountBalances();
                    break;
                case "5":
                    handleListCategories();
                    break;
                case "6":
                    handleImportCsvTransactions();
                    break;
                case "7":
                    handleSetBudget();
                    break;
                case "8":
                    handleViewBudgetStatus();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    log.warn("Invalid choice. Please try again.");
            }
            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine(); // Pause
            }
        }
        log.info("Exiting Budget CLI. Goodbye!");
        scanner.close();
    }

    private void displayMenu() {
        System.out.println("\n--- Budget CLI Menu ---");
        System.out.println("1. Add Income");
        System.out.println("2. Add Expense");
        System.out.println("3. View Transactions for Account");
        System.out.println("4. View Account Balances");
        System.out.println("5. List Categories");
        System.out.println("6. Import Transactions from CSV");
        System.out.println("7. Set/Update Budget for Category");
        System.out.println("8. View Budget Status for Month");
        System.out.println("0. Exit");
        System.out.println("-----------------------");
    }

    private void handleAddTransaction(TransactionType type) {
        log.info("--- Add {} ---", type.getDisplayName());
        try {
            System.out.print("Enter Amount: ");
            BigDecimal amount = new BigDecimal(scanner.nextLine().trim());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Amount must be positive.");
                return;
            }

            System.out.print("Enter Transaction Date (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(scanner.nextLine().trim(), DateTimeFormatter.ISO_LOCAL_DATE);

            System.out.print("Enter Description (optional): ");
            String description = scanner.nextLine().trim();

            // Select Account
            List<Account> accounts = accountService.getAllAccounts();
            if (accounts.isEmpty()) {
                log.warn("No accounts available. Please add an account first.");
                return;
            }
            log.info("Available Accounts:");
            for (int i = 0; i < accounts.size(); i++) {
                System.out.printf("%d. %s (Balance: %.2f)\n", i + 1, accounts.get(i).getName(), accounts.get(i).getBalance());
            }
            System.out.print("Select Account (number): ");
            int accChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (accChoice < 0 || accChoice >= accounts.size()) {
                log.warn("Invalid account selection.");
                return;
            }
            Account selectedAccount = accounts.get(accChoice);

            Long categoryId = null;
            List<Category> categories = categoryService.getAllCategories();
            if (!categories.isEmpty()) {
                log.info("Available Categories:");
                for (int i = 0; i < categories.size(); i++) {
                    System.out.printf("%d. %s\n", i + 1, categories.get(i).getName());
                }
                System.out.print("Select Category (number, or 0 for none): ");
                int catChoice = Integer.parseInt(scanner.nextLine().trim());
                if (catChoice > 0 && catChoice <= categories.size()) {
                    categoryId = categories.get(catChoice - 1).getId();
                } else if (catChoice != 0) {
                    log.warn("Invalid category selection, proceeding without category.");
                }
            } else {
                log.info("No categories available.");
            }

            if (type == TransactionType.EXPENSE && categoryId == null) {
                log.warn("Expense transactions should ideally have a category. For now, proceeding without one if none selected.");
                // You might want to make category mandatory for expenses here.
            }

            transactionService.createTransaction(amount, type, date, description, categoryId, selectedAccount.getId());
            Account updatedAccount = accountService.findAccountById(selectedAccount.getId())
                            .orElse(selectedAccount);
            log.info("{} of {} added successfully to account '{}'. New balance: {}",
                    type.getDisplayName(), amount, selectedAccount.getName(), accountService.findAccountById(selectedAccount.getId()).get().getBalance());

        } catch (NumberFormatException e) {
            log.warn("Invalid number format for amount or choice.");
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format. Please use YYYY-MM-DD.");
        } catch (Exception e) {
            log.error("Error adding transaction: {}", e.getMessage(), e);
        }
    }

    private void handleViewAccountTransactions() {
        log.info("--- View Transactions for Account ---");
        try {
            List<Account> accounts = accountService.getAllAccounts();
            if (accounts.isEmpty()) {
                log.warn("No accounts available.");
                return;
            }
            log.info("Available Accounts:");
            for (int i = 0; i < accounts.size(); i++) {
                System.out.printf("%d. %s\n", i + 1, accounts.get(i).getName());
            }
            System.out.print("Select Account (number): ");
            int accChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (accChoice < 0 || accChoice >= accounts.size()) {
                log.warn("Invalid account selection.");
                return;
            }
            Account selectedAccount = accounts.get(accChoice);

            List<Transaction> transactions = transactionService.getTransactionsByAccountId(selectedAccount.getId());
            if (transactions.isEmpty()) {
                log.info("No transactions found for account: {}", selectedAccount.getName());
            } else {
                log.info("Transactions for {}:", selectedAccount.getName());
                transactions.forEach(t -> log.info(
                        "  ID: {}, Date: {}, Type: {}, Amt: {:.2f}, Cat: {}, Desc: {}",
                        t.getId(), t.getTransactionDate(), t.getTransactionType().getDisplayName(),
                        t.getAmount(), (t.getCategory() != null ? t.getCategory().getName() : "N/A"), t.getDescription()
                ));
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid account selection format.");
        } catch (Exception e) {
            log.error("Error viewing transactions: {}", e.getMessage(), e);
        }
    }

    private void handleViewAccountBalances() {
        log.info("--- Account Balances ---");
        List<Account> accounts = accountService.getAllAccounts();
        if (accounts.isEmpty()) {
            log.info("No accounts found.");
        } else {
            accounts.forEach(acc ->
                    log.info("Account: '{}', Type: '{}', Balance: {:.2f}",
                            acc.getName(), acc.getAccountType().getDisplayName(), acc.getBalance())
            );
        }
    }

    private void handleListCategories() {
        log.info("--- Categories ---");
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            log.info("No categories found.");
        } else {
            categories.forEach(cat -> log.info("Category - ID: {}, Name: {}", cat.getId(), cat.getName()));
        }
    }

    private void handleImportCsvTransactions() {
        log.info("--- Import Transactions from CSV ---");
        try {
            System.out.print("Enter the full path to the CSV file: ");
            String filePath = scanner.nextLine().trim();

            File csvFile = new File(filePath);
            if (!csvFile.exists() || !csvFile.isFile()) {
                log.warn("File not found or is not in the expected format or file type: {}", filePath);
                return;
            }

            List<Account> accounts = accountService.getAllAccounts();
            if (accounts.isEmpty()) {
                log.warn("No accounts available to import transactions into. Please add an account first.");
                return;
            }

            log.info("Available Accounts to import into: ");
            for (int i = 0; i < accounts.size(); i++){
                System.out.printf("%d. %s\n", i + 1, accounts.get(i).getName());
            }
            System.out.print("Select Account (number) for this CSV import: ");
            int accChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;

            if (accChoice < 0 || accChoice >= accounts.size()) {
                log.warn("Invalid account selection:");
                return;
            }
            Account targetAccount = accounts.get(accChoice);

            log.info("Importing transactions from '{}' into account '{}'...", csvFile.getName(), targetAccount.getName());

            try (InputStream inputStream = new FileInputStream(csvFile)) {
                ImportSummary summary = csvImportService.importTransactionsFromCsv(inputStream, targetAccount.getId(), csvFile.getName());

                log.info("--- CSV Import Summary for '{}' ---", csvFile.getName());
                log.info("Total data rows read: {}", summary.totalRowsRead());
                log.info("Successfully imported transactions: {}", summary.successfulImports());
                log.info("Failed imports: {}", summary.failedImports());
                if (summary.failedImports() > 0 && !summary.errorMessages().isEmpty()) {
                    log.warn("Errors encountered during import:");
                    summary.errorMessages().forEach(log::error);
                } else if (summary.failedImports() == -1 && !summary.errorMessages().isEmpty()) {
                    log.error("Critical error reading CSV file:");
                    summary.errorMessages().forEach(log::error);
                }
                log.info("-------------------------------------");

                Account updatedTargetAccount = accountService.findAccountById(targetAccount.getId())
                        .orElse(targetAccount);
                log.info("New balance for account '{}' after import: {}", updatedTargetAccount.getName(), String.format("%.2f", updatedTargetAccount.getBalance()));

            } catch (FileNotFoundException e) {
                log.error("Error: CSV file not found at path '{}'", filePath);

            } catch (IOException e) {
                log.error("Error reading CSV file: '{}': {}", filePath, e.getMessage(), e);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid number format for account selection.");
        } catch (Exception e) {
            log.error("An unexpected error occurred during CSV import process: {}", e.getMessage(), e);
        }
    }

    private void handleSetBudget() {
        log.info("--- Set/Update Budget for Category ---");
        try {
            List<Category> categories = categoryService.getAllCategories();
            if (categories.isEmpty()) {
                log.warn("No categories available. Please add categories first.");
                return;
            }
            log.info("Available Categories:");
            for (int i = 0; i < categories.size(); i++) {
                System.out.printf("%d. %s\n", i + 1, categories.get(i).getName());
            }
            System.out.print("Select Category (number) for budget: ");
            int catChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (catChoice < 0 || catChoice >= categories.size()) {
                log.warn("Invalid category selection:");
                return;
            }
            Category selectedCategory = categories.get(catChoice);
            System.out.print("Enter Year (e.g., 2025): ");
            int year = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Month (1-12): ");
            int month = Integer.parseInt(scanner.nextLine().trim());
            if (month < 1 || month > 12) {
                log.warn("Invalid month selection. Please enter a number between 1 and 12.");
                return;
            }
            System.out.print("Enter Budgeted Amount for '" + selectedCategory.getName() + "' for " + year + "-" + String.format("%02d", month) + ": ");
            BigDecimal budgetedAmount = new BigDecimal(scanner.nextLine().trim());

            if (budgetedAmount.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Budgeted amount cannot be negative.");
                return;
            }

            budgetService.setBudget (selectedCategory.getId(), year,month, budgetedAmount);
            log.info("Budget successfully set for Category '{}' for {}-{} to {}", selectedCategory.getName(), year, month, budgetedAmount);
        } catch (NumberFormatException e) {
            log.warn("Invalid number format for choice, year, month or amount.");
        } catch (Exception e) {
            log.error("Error setting budget: {}", e.getMessage(), e);
        }
    }

    private void handleViewBudgetStatus() {
        log.info("--- View Budget Status for Month ---");
        try {
            System.out.print("Enter Year (e.g., 2025): ");
            int year = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Month (1-12): ");
            int month = Integer.parseInt(scanner.nextLine().trim());
            if (month < 1 || month > 12) {
                log.warn("Invalid month. Please enter a number between 1 and 12.");
                return;
            }

            log.info("Budget Status for {}-{}:", year, month);
            List<BudgetStatusDTO> budgetStatuses = budgetService.getOverallBudgetStatusForPeriod(year, month);

            if (budgetStatuses.isEmpty()) {
                log.info("No budgets set for this period, or no spending in budgeted categories.");

            } else {
                System.out.println("--------------------------------------------------------------------------");
                System.out.printf("%-20s | %12s | %12s | %12s%n", "Category", "Budgeted", "Spent", "Remaining");
                System.out.println("--------------------------------------------------------------------------");
                for (BudgetStatusDTO status : budgetStatuses) {
                    System.out.printf("%-20s | %12.2f | %12.2f | %12.2f%n",
                            status.categoryName(),
                            status.budgetedAmount(),
                            status.actualSpending(),
                            status.remainingOrOverspent());
                }
                System.out.println("--------------------------------------------------------------------------");
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid number format for year or month.");
        } catch (Exception e) {
            log.error("Error viewing budget status: {}", e.getMessage(), e);
        }
    }
}