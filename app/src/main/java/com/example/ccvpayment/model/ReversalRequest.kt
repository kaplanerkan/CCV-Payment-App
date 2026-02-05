package com.example.ccvpayment.model

/**
 * İptal (Reversal) Talebi
 */
data class ReversalRequest(
    val originalRequestId: String,
    val amount: Money? = null
)
