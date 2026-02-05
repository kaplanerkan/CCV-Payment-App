package com.example.ccvpayment.model

import java.util.Date

/**
 * Ödeme Sonucu
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
    val isSuccess: Boolean get() = status == TransactionStatus.SUCCESS
    val isPartialApproval: Boolean get() = approvedAmount != null && amount != null && approvedAmount.amount < amount.amount
}
