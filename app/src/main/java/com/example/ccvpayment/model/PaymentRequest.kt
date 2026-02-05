package com.example.ccvpayment.model

/**
 * Ödeme Talebi
 */
data class PaymentRequest(
    val amount: Money,
    val type: PaymentType = PaymentType.SALE,
    val requestId: String = System.currentTimeMillis().toString(),
    val reference: String? = null,
    val allowPartialApproval: Boolean = false,
    val tipAmount: Money? = null
)
