package com.example.ccvpayment.model

/**
 * İade Talebi
 */
data class RefundRequest(
    val amount: Money,
    val originalTransactionId: String? = null,
    val reason: String? = null,
    val requestId: String = System.currentTimeMillis().toString()
)
