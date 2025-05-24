package com.budget.budgetappspringboot.runner;

import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.entity.Transaction;
import com.budget.budgetappspringboot.model.enums.TransactionType;
import com.budget.budgetappspringboot.service.AccountService;
import com.budget.budgetappspringboot.service.CategoryService;
import com.budget.budgetappspringboot.service.TransactionService;
import com.budget.budgetappspringboot.service.impl.AccountServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.budget.budgetappspringboot.dto.ImportSummary;
import com.budget.budgetappspringboot.service.CsvImportService;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Component
@Slf4j
@Order(10) // Run after data seeders (if any are active with lower order numbers)
public class InteractiveBudgetCLI implements CommandLineRunner {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final CsvImportService csvImportService;
    private final Scanner scanner; // For reading user input
    private final AccountServiceImpl accountServiceImpl;

    public InteractiveBudgetCLI(TransactionService transactionService,
                                AccountService accountService,
                                CategoryService categoryService,
                                CsvImportService csvImportService, AccountServiceImpl accountServiceImpl) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.csvImportService = csvImportService;
        this.scanner = new Scanner(System.in); // Initialize scanner
        this.accountServiceImpl = accountServiceImpl;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Welcome to the Interactive Budget CLI!");
        // Temporarily disable other data runners if you don't want them to run during interactive mode
        // or ensure their @Order is lower than this one.

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
        scanner.close(); // Close the scanner when done
    }

    private void displayMenu() {
        System.out.println("\n--- Budget CLI Menu ---");
        System.out.println("1. Add Income");
        System.out.println("2. Add Expense");
        System.out.println("3. View Transactions for Account");
        System.out.println("4. View Account Balances");
        System.out.println("5. List Categories");
        System.out.println("6. Import Transactions from CSV");
        System.out.println("0. Exit");
        System.out.println("-----------------------");
    }

    // --- Placeholder methods for menu actions ---
    // We will implement these one by one

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

            // Select Category (optional for income, maybe mandatory for expense)
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
}