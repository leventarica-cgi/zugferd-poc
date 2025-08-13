
package com.example.zugferd;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CanonicalInvoice(
    String number,
    LocalDate issueDate,
    String currency,
    Party seller,
    Party buyer,
    BigDecimal taxRate,
    List<LineItem> lines,
    String notes,
    String paymentTerms,
    String bankName,
    String iban,
    String bic
) {}
