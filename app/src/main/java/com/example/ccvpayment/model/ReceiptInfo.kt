package com.example.ccvpayment.model

/**
 * Receipt information from a payment transaction.
 *
 * Contains the text content of various receipt types that can be
 * printed or displayed after a transaction completes.
 *
 * @property merchantReceipt The merchant's copy of the receipt
 * @property customerReceipt The customer's copy of the receipt
 * @property journalReceipt The journal/audit copy of the receipt
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class ReceiptInfo(
    val merchantReceipt: String?,
    val customerReceipt: String?,
    val journalReceipt: String?
)
