package com.example.ccvpayment.model

import java.math.BigDecimal
import java.util.Currency

/**
 * Represents a monetary amount with currency.
 *
 * This data class encapsulates an amount and its associated currency,
 * providing a type-safe way to handle money values in payment operations.
 *
 * @property amount The monetary value as a BigDecimal for precision
 * @property currency The currency of the amount (defaults to EUR)
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class Money(
    val amount: BigDecimal,
    val currency: Currency = Currency.getInstance("EUR")
) {
    /**
     * Formats the money value as a human-readable string.
     *
     * @return The formatted string with amount and currency symbol (e.g., "10.50 €")
     */
    fun formatted(): String {
        return String.format("%.2f %s", amount, currency.symbol)
    }
}
