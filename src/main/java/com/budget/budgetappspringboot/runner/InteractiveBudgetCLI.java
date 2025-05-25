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
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Comparator;

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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

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
                case "9":
                    handleViewTransactionsByCategoryAndDateRange();
                    break;
                case "10":
                    handleViewAllTransactionsByDateRange();
                    break;
                case "11": // <<< NEW CASE
                    handleViewTransactionsByTypeAndDateRange();
                    break;
                case "12": // <<< NEW CASE
                    handleSearchTransactionsByDescription();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    log.warn("Invalid choice. Please try again.");
            }
            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
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
        System.out.println("9. View Transactions by Category & Date Range");
        System.out.println("10. View All Transactions by Date Range");
        System.out.println("11. View Transactions by Type & Date Range");
        System.out.println("12. Search Transactions by Description Keyword");
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

    private void handleViewTransactionsByCategoryAndDateRange() {
        log.info("--- View Transactions by Category & Date Range ---");
        try {
            // 1. Select Category
            List<Category> categories = categoryService.getAllCategories();
            if (categories.isEmpty()) {
                log.warn("No categories available to filter by.");
                return;
            }
            log.info("Available Categories:");
            for (int i = 0; i < categories.size(); i++) {
                System.out.printf("%d. %s\n", i + 1, categories.get(i).getName());
            }
            System.out.print("Select Category (number): ");
            int catChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (catChoice < 0 || catChoice >= categories.size()) {
                log.warn("Invalid category selection.");
                return;
            }
            Category selectedCategory = categories.get(catChoice);

            // 2. Get Dates
            System.out.print("Enter Start Date (YYYY-MM-DD): ");
            LocalDate startDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);

            System.out.print("Enter End Date (YYYY-MM-DD): ");
            LocalDate endDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);

            if (startDate.isAfter(endDate)) {
                log.warn("Start date cannot be after end date.");
                return;
            }

            // 3. Call Service
            List<Transaction> transactions = transactionService.getTransactionsByCategoryAndDateRange(
                    selectedCategory.getId(), startDate, endDate);

            // 4. Display Results
            if (transactions.isEmpty()) {
                log.info("No transactions found for category '{}' between {} and {}.",
                        selectedCategory.getName(),
                        startDate.format(DATE_FORMATTER),
                        endDate.format(DATE_FORMATTER));
            } else {
                log.info("Transactions for category '{}' between {} and {}:",
                        selectedCategory.getName(),
                        startDate.format(DATE_FORMATTER),
                        endDate.format(DATE_FORMATTER));
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.printf("%-12s | %-10s | %-8s | %10s | %-15s | %-30s%n",
                        "Account", "Date", "Type", "Amount", "Category", "Description");
                System.out.println("----------------------------------------------------------------------------------------------------");
                transactions.forEach(t -> System.out.printf("%-12.12s | %-10s | %-8s | %10.2f | %-15.15s | %-30.30s%n",
                        t.getAccount().getName(), // Assumes Account name is not too long
                        t.getTransactionDate().format(DATE_FORMATTER),
                        t.getTransactionType().getDisplayName(),
                        t.getAmount(),
                        (t.getCategory() != null ? t.getCategory().getName() : "N/A"), // Category name should be selectedCategory.getName()
                        t.getDescription()
                ));
                System.out.println("----------------------------------------------------------------------------------------------------");
            }

        } catch (NumberFormatException e) {
            log.warn("Invalid number format for category choice.");
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format. Please use YYYY-MM-DD.");
        } catch (NoSuchElementException e) { // If service throws this for category not found (shouldn't happen if selected from list)
            log.error("Error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error viewing transactions by category and date: {}", e.getMessage(), e);
        }
    }

    private void handleViewAllTransactionsByDateRange() {
        log.info("--- View All Transactions by Date Range ---");
        try {
            System.out.print("Enter Start Date (YYYY-MM-DD): ");
            LocalDate startDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);

            System.out.print("Enter End Date (YYYY-MM-DD): ");
            LocalDate endDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);

            if (startDate.isAfter(endDate)) {
                log.warn("Start date cannot be after end date.");
                return;
            }

            List<Transaction> transactions = transactionService.getAllTransactionsByDateRange(startDate, endDate);

            if (transactions.isEmpty()) {
                log.info("No transactions found between {} and {}.",
                        startDate.format(DATE_FORMATTER),
                        endDate.format(DATE_FORMATTER));
                return;
            }

            System.out.print("Sort results? (yes/no, default: no - uses repository default sort): ");
            String sortChoice = scanner.nextLine().trim().toLowerCase();

            if ("yes".equals(sortChoice)) {
                System.out.println("Sort by:");
                System.out.println("1. Date");
                System.out.println("2. Amount");
                System.out.print("Enter field to sort by (1 or 2): ");
                String fieldChoice = scanner.nextLine().trim();

                System.out.println("Sort direction:");
                System.out.println("1. Ascending");
                System.out.println("2. Descending");
                System.out.print("Enter sort direction (1 or 2): ");
                String dirChoice = scanner.nextLine().trim();

                Comparator<Transaction> comparator = null;

                if ("1".equals(fieldChoice)) { // Sort by Date
                    comparator = Comparator.comparing(Transaction::getTransactionDate);
                    log.info("Sorting by Date.");
                } else if ("2".equals(fieldChoice)) { // Sort by Amount
                    comparator = Comparator.comparing(Transaction::getAmount);
                    log.info("Sorting by Amount.");
                } else {
                    log.warn("Invalid sort field selected. Displaying with default repository order.");
                }

                if (comparator != null) {
                    if ("2".equals(dirChoice)) { // Descending
                        comparator = comparator.reversed();
                        log.info("Sort direction: Descending.");
                    } else {
                        log.info("Sort direction: Ascending.");
                    }
                    transactions.sort(comparator);
                }
            }
                log.info("All transactions between {} and {}:",
                        startDate.format(DATE_FORMATTER),
                        endDate.format(DATE_FORMATTER));
                // Using the same display format as other transaction listings
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.printf("%-12s | %-10s | %-8s | %10s | %-15s | %-30s%n",
                        "Account", "Date", "Type", "Amount", "Category", "Description");
                System.out.println("----------------------------------------------------------------------------------------------------");
                transactions.forEach(t -> System.out.printf("%-12.12s | %-10s | %-8s | %10.2f | %-15.15s | %-30.30s%n",
                        t.getAccount().getName(),
                        t.getTransactionDate().format(DATE_FORMATTER),
                        t.getTransactionType().getDisplayName(),
                        t.getAmount(),
                        (t.getCategory() != null ? t.getCategory().getName() : "N/A"),
                        t.getDescription()
                ));
                System.out.println("----------------------------------------------------------------------------------------------------");

        } catch (DateTimeParseException e) {
            log.warn("Invalid date format. Please use YYYY-MM-DD.");
        } catch (NumberFormatException e) {
            log.warn("INvalid numeric choice for sorting options.");
        } catch (Exception e) {
            log.error("Error viewing transactions by date range: {}", e.getMessage(), e);
        }
    }

    private void handleViewTransactionsByTypeAndDateRange() {
        log.info("--- View Transactions by Type & Date Range ---");
        try {
            // 1. Select Transaction Type
            System.out.println("Select Transaction Type:");
            System.out.println("1. Income");
            System.out.println("2. Expense");
            System.out.print("Enter choice (1 or 2): ");
            String typeChoiceStr = scanner.nextLine().trim();
            TransactionType selectedType;
            if ("1".equals(typeChoiceStr)) {
                selectedType = TransactionType.INCOME;
            } else if ("2".equals(typeChoiceStr)) {
                selectedType = TransactionType.EXPENSE;
            } else {
                log.warn("Invalid transaction type choice.");
                return;
            }

            // 2. Get Dates
            System.out.print("Enter Start Date (YYYY-MM-DD): ");
            LocalDate startDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);

            System.out.print("Enter End Date (YYYY-MM-DD): ");
            LocalDate endDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);

            if (startDate.isAfter(endDate)) {
                log.warn("Start date cannot be after end date.");
                return;
            }

            // 3. Call Service
            List<Transaction> transactions = transactionService.getTransactionsByTypeAndDateRange(
                    selectedType, startDate, endDate);

            // 4. Display Results
            if (transactions.isEmpty()) {
                log.info("No {} transactions found between {} and {}.",
                        selectedType.getDisplayName(),
                        startDate.format(DATE_FORMATTER),
                        endDate.format(DATE_FORMATTER));
            } else {
                log.info("{} transactions between {} and {}:",
                        selectedType.getDisplayName(),
                        startDate.format(DATE_FORMATTER),
                        endDate.format(DATE_FORMATTER));
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.printf("%-12s | %-10s | %-8s | %10s | %-15s | %-30s%n",
                        "Account", "Date", "Type", "Amount", "Category", "Description");
                System.out.println("----------------------------------------------------------------------------------------------------");
                transactions.forEach(t -> System.out.printf("%-12.12s | %-10s | %-8s | %10.2f | %-15.15s | %-30.30s%n",
                        t.getAccount().getName(),
                        t.getTransactionDate().format(DATE_FORMATTER),
                        t.getTransactionType().getDisplayName(),
                        t.getAmount(),
                        (t.getCategory() != null ? t.getCategory().getName() : "N/A"),
                        t.getDescription()
                ));
                System.out.println("----------------------------------------------------------------------------------------------------");
            }

        } catch (NumberFormatException e) { // For type choice
            log.warn("Invalid choice format. Please enter a number.");
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format. Please use YYYY-MM-DD.");
        } catch (Exception e) {
            log.error("Error viewing transactions by type and date: {}", e.getMessage(), e);
        }
    }

    private void handleSearchTransactionsByDescription() {
        log.info("--- Search Transactions by Description Keyword ---");
        try {
            System.out.print("Enter keyword to search in description: ");
            String keyword = scanner.nextLine().trim();

            if (keyword.isEmpty()) {
                log.warn("Search keyword cannot be empty.");
                return;
            }

            List<Transaction> transactions = transactionService.searchTransactionsByDescription(keyword);

            if (transactions.isEmpty()) {
                log.info("No transactions found with description containing '{}'.", keyword);
            } else {
                log.info("Transactions with description containing '{}':", keyword);
                // Using the same display format as other transaction listings
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.printf("%-12s | %-10s | %-8s | %10s | %-15s | %-30s%n",
                        "Account", "Date", "Type", "Amount", "Category", "Description");
                System.out.println("----------------------------------------------------------------------------------------------------");
                transactions.forEach(t -> System.out.printf("%-12.12s | %-10s | %-8s | %10.2f | %-15.15s | %-30.30s%n",
                        t.getAccount().getName(),
                        t.getTransactionDate().format(DATE_FORMATTER),
                        t.getTransactionType().getDisplayName(),
                        t.getAmount(),
                        (t.getCategory() != null ? t.getCategory().getName() : "N/A"),
                        t.getDescription()
                ));
                System.out.println("----------------------------------------------------------------------------------------------------");
            }
        } catch (Exception e) {
            log.error("Error searching transactions by description: {}", e.getMessage(), e);
        }
    }
}