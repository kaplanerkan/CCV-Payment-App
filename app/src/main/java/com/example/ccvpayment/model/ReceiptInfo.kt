package com.example.ccvpayment.model

/**
 * Fiş Bilgisi
 */
data class ReceiptInfo(
    val merchantReceipt: String?,
    val customerReceipt: String?,
    val journalReceipt: String?
)
