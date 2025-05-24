package com.budget.budgetappspringboot.service;

import com.budget.budgetappspringboot.dto.ImportSummary;
import java.io.InputStream;

public interface CsvImportService {
    ImportSummary importTransactionsFromCsv(InputStream inputStream, Long targetAccountId, String originalFileName);
}
