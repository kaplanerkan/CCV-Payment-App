package com.example.ccvpayment.model

import java.util.Date

/**
 * İşlem Geçmişi Bilgisi
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
