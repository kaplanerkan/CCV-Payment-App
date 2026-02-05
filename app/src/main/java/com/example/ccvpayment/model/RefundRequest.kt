package com.example.ccvpayment.model

/**
 * Request object for initiating a refund transaction.
 *
 * Contains all the necessary information to process a refund
 * through the CCV terminal.
 *
 * @property amount The amount to refund (required)
 * @property originalTransactionId Optional ID of the original transaction being refunded
 * @property reason Optional reason for the refund
 * @property requestId Unique identifier for this request (auto-generated if not provided)
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class RefundRequest(
    val amount: Money,
    val originalTransactionId: String? = null,
    val reason: String? = null,
    val requestId: String = System.currentTimeMillis().toString()
)
