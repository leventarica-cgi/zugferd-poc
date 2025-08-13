
package com.example.zugferd;

import java.math.BigDecimal;

public record LineItem(
    String name,
    BigDecimal qty,
    String unit,
    BigDecimal netUnitPrice
) {}
