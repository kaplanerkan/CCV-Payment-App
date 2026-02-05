package com.example.ccvpayment.model

import java.math.BigDecimal
import java.util.Currency

/**
 * Para Birimi Modeli
 */
data class Money(
    val amount: BigDecimal,
    val currency: Currency = Currency.getInstance("EUR")
) {
    fun formatted(): String {
        return String.format("%.2f %s", amount, currency.symbol)
    }
}
