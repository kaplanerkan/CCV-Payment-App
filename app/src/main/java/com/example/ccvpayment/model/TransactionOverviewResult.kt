package com.example.ccvpayment.model

/**
 * İşlem Özeti
 */
data class TransactionOverviewResult(
    val success: Boolean,
    val shiftNumber: Int?,
    val transactions: List<TransactionInfo>,
    val errorMessage: String? = null
)
