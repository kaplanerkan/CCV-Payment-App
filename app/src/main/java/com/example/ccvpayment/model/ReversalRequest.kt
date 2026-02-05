package com.example.ccvpayment.model

/**
 * Request object for reversing/voiding a previous transaction.
 *
 * Used to cancel a transaction that has already been processed.
 * The reversal must reference the original transaction's request ID.
 *
 * @property originalRequestId The request ID of the transaction to reverse (required)
 * @property amount Optional amount for partial reversal (full reversal if not specified)
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class ReversalRequest(
    val originalRequestId: String,
    val amount: Money? = null
)
