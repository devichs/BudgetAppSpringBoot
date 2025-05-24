package com.budget.budgetappspringboot.service.impl;

import com.budget.budgetappspringboot.dto.ImportSummary;
import com.budget.budgetappspringboot.entity.Account;
import com.budget.budgetappspringboot.entity.Category;
import com.budget.budgetappspringboot.model.enums.TransactionType;
import com.budget.budgetappspringboot.repository.AccountRepository; // To fetch the target Account
import com.budget.budgetappspringboot.service.CategoryService;
import com.budget.budgetappspringboot.service.CsvImportService;
import com.budget.budgetappspringboot.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CsvImportServiceImpl implements CsvImportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

    private final AccountRepository accountRepository;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    public CsvImportServiceImpl(AccountRepository accountRepository,
                                CategoryService categoryService,
                                TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
    }

    @Override
    public ImportSummary importTransactionsFromCsv(InputStream inputStream, Long targetAccountId, String originalFileName) {
        log.info("Starting CSV import for account ID: {} from file: {}", targetAccountId, originalFileName);

        Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> {
                    log.error("Target account with ID {} not found for CSV import.", targetAccountId);
                    return new EntityNotFoundException("Target account not found with ID: " + targetAccountId);
                });

        List<String> errorMessages = new ArrayList<>();
        int totalRowsRead = 0;
        int successfulImports = 0;
        int failedImports = 0;

        // Expected headers - your standardized format
        String[] expectedHeaders = {"Date", "Description", "Category", "Amount"};

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withHeader(expectedHeaders) // Define expected headers
                     .withFirstRecordAsHeader()   // Use the first record as header names
                     .withTrim()                  // Trim whitespace from values
                     .withIgnoreEmptyLines(true))) {

            for (CSVRecord csvRecord : csvParser) {
                totalRowsRead++;
                try {
                    String dateStr = csvRecord.get("Date");
                    String description = csvRecord.get("Description");
                    String categoryName = csvRecord.get("Category");
                    String amountStr = csvRecord.get("Amount");

                    // Basic validation
                    if (dateStr.isEmpty() || amountStr.isEmpty()) {
                        throw new IllegalArgumentException("Date or Amount is empty.");
                    }

                    LocalDate transactionDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                    BigDecimal amount = new BigDecimal(amountStr);

                    TransactionType transactionType = (amount.compareTo(BigDecimal.ZERO) < 0) ?
                            TransactionType.EXPENSE : TransactionType.INCOME;
                    BigDecimal absoluteAmount = amount.abs(); // Store absolute amount

                    Category category = categoryService.findOrCreateCategory(categoryName);
                    if (categoryName != null && !categoryName.isEmpty() && category == null) {
                        // This case might occur if findOrCreateCategory returns null for empty/invalid names
                        // and you want to enforce a category or handle it as an error.
                        // For now, findOrCreateCategory handles creation or returns existing.
                        // If category is truly mandatory and findOrCreateCategory could return null for valid reasons:
                        // throw new IllegalArgumentException("Category '" + categoryName + "' could not be processed.");
                    }

                    Long categoryIdToUse = (category != null) ? category.getId() : null;

                    transactionService.createTransaction(
                            absoluteAmount,
                            transactionType,
                            transactionDate,
                            description,
                            categoryIdToUse, // Pass category ID
                            targetAccount.getId()
                    );
                    successfulImports++;

                } catch (DateTimeParseException e) {
                    failedImports++;
                    String errorMsg = String.format("Row %d: Invalid date format for '%s'. Expected YYYY-MM-DD. Error: %s", csvRecord.getRecordNumber(), csvRecord.get("Date"), e.getMessage());
                    log.warn(errorMsg);
                    errorMessages.add(errorMsg);
                } catch (NumberFormatException e) {
                    failedImports++;
                    String errorMsg = String.format("Row %d: Invalid amount format for '%s'. Error: %s", csvRecord.getRecordNumber(), csvRecord.get("Amount"), e.getMessage());
                    log.warn(errorMsg);
                    errorMessages.add(errorMsg);
                } catch (IllegalArgumentException | EntityNotFoundException e) {
                    failedImports++;
                    String errorMsg = String.format("Row %d: Data validation error or entity not found. Record: [%s]. Error: %s", csvRecord.getRecordNumber(), csvRecord.toString(), e.getMessage());
                    log.warn(errorMsg);
                    errorMessages.add(errorMsg);
                } catch (Exception e) { // Catch any other unexpected errors for a row
                    failedImports++;
                    String errorMsg = String.format("Row %d: Unexpected error processing record [%s]. Error: %s", csvRecord.getRecordNumber(), csvRecord.toString(), e.getMessage());
                    log.error(errorMsg, e);
                    errorMessages.add(errorMsg);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read or parse CSV file: {}", originalFileName, e);
            // For an IOException during file reading, we might not have row-specific errors yet.
            // Add a general error message.
            errorMessages.add("Error reading CSV file: " + e.getMessage());
            // In this case, totalRowsRead might be 0 or reflect partial reading.
            // You might decide to set failedImports to totalRowsRead if processing couldn't even start properly.
            if (totalRowsRead == 0) failedImports = -1; // Indicate a general file read error
        }

        log.info("CSV import finished for file: {}. Total rows read: {}, Successful: {}, Failed: {}",
                originalFileName, totalRowsRead, successfulImports, failedImports);
        return new ImportSummary(totalRowsRead, successfulImports, failedImports, errorMessages);
    }
}