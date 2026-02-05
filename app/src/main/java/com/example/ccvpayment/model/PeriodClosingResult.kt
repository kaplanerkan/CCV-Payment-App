package com.example.ccvpayment.model

import java.util.Date

/**
 * Result of a period closing operation (Z-Report or X-Report).
 *
 * Contains summary information about all transactions processed
 * during the shift period. Z-Report closes the shift while X-Report
 * provides a summary without closing.
 *
 * @property success Whether the period closing was successful
 * @property shiftNumber The shift/batch number being closed
 * @property totalTransactions Total number of transactions in the period
 * @property totalSalesCount Number of sale transactions
 * @property totalSalesAmount Sum of all sale amounts
 * @property totalRefundsCount Number of refund transactions
 * @property totalRefundsAmount Sum of all refund amounts
 * @property netAmount Net amount (sales minus refunds)
 * @property receipt The printed report receipt text
 * @property closingTime When the period was closed
 * @property errorMessage Error description if closing failed
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class PeriodClosingResult(
    val success: Boolean,
    val shiftNumber: Int?,
    val totalTransactions: Int,
    val totalSalesCount: Int,
    val totalSalesAmount: Money?,
    val totalRefundsCount: Int,
    val totalRefundsAmount: Money?,
    val netAmount: Money?,
    val receipt: String?,
    val closingTime: Date = Date(),
    val errorMessage: String? = null
)
