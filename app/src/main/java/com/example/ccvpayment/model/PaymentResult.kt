package com.example.ccvpayment.model

import java.util.Date

/**
 * Result object returned after a payment transaction is processed.
 *
 * Contains comprehensive information about the completed transaction
 * including status, card details, receipts, and any error information.
 *
 * @property status The outcome of the transaction
 * @property transactionId Unique ID assigned by the payment processor
 * @property requestId The original request ID that was submitted
 * @property amount The requested transaction amount
 * @property approvedAmount The actual approved amount (may differ for partial approvals)
 * @property cardBrand The card brand used (Visa, Mastercard, etc.)
 * @property maskedPan The masked card number (e.g., "****1234")
 * @property authCode Authorization code from the card issuer
 * @property merchantReceipt The merchant copy of the receipt
 * @property customerReceipt The customer copy of the receipt
 * @property errorCode Error code if the transaction failed
 * @property errorMessage Human-readable error description
 * @property timestamp When the transaction was processed
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class PaymentResult(
    val status: TransactionStatus,
    val transactionId: String? = null,
    val requestId: String? = null,
    val amount: Money? = null,
    val approvedAmount: Money? = null,
    val cardBrand: String? = null,
    val maskedPan: String? = null,
    val authCode: String? = null,
    val merchantReceipt: String? = null,
    val customerReceipt: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val timestamp: Date = Date()
) {
    /** Returns true if the transaction was successful */
    val isSuccess: Boolean get() = status == TransactionStatus.SUCCESS

    /** Returns true if only a partial amount was approved */
    val isPartialApproval: Boolean get() = approvedAmount != null && amount != null && approvedAmount.amount < amount.amount
}
