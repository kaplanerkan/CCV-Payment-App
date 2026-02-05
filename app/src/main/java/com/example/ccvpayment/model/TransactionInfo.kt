package com.example.ccvpayment.model

import java.util.Date

/**
 * Information about a single transaction in the transaction history.
 *
 * Contains details about a completed payment transaction that can be
 * used for reporting, reconciliation, or reference purposes.
 *
 * @property transactionId Unique identifier for the transaction
 * @property type The type of transaction (sale, refund, etc.)
 * @property status The outcome of the transaction
 * @property amount The transaction amount
 * @property cardBrand The card brand used (Visa, Mastercard, etc.)
 * @property maskedPan The masked card number
 * @property timestamp When the transaction was processed
 * @property shiftNumber The shift/batch number when transaction occurred
 * @property authCode Authorization code from the issuer
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class TransactionInfo(
    val transactionId: String,
    val type: PaymentType,
    val status: TransactionStatus,
    val amount: Money,
    val cardBrand: String?,
    val maskedPan: String?,
    val timestamp: Date,
    val shiftNumber: Int?,
    val authCode: String?
)
