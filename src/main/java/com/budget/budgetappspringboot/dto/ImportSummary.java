package com.budget.budgetappspringboot.dto;

import java.util.List;

public record ImportSummary(
        int totalRowsRead,
        int successfulImports,
        int failedImports,
        List<String> errorMessages
) { }
