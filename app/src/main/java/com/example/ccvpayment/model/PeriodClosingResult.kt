package com.example.ccvpayment.model

import java.util.Date

/**
 * Dönem Kapama Sonucu (Z-Raporu / X-Raporu)
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
