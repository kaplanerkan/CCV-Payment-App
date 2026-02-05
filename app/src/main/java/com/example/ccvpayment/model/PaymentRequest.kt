package com.example.ccvpayment.model

/**
 * Request object for initiating a payment transaction.
 *
 * Contains all the necessary information to start a payment
 * through the CCV terminal.
 *
 * @property amount The amount to charge (required)
 * @property type The type of payment transaction (defaults to SALE)
 * @property requestId Unique identifier for this request (auto-generated if not provided)
 * @property reference Optional reference number for the transaction
 * @property allowPartialApproval Whether to allow partial approval if full amount unavailable
 * @property tipAmount Optional tip amount to add to the transaction
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class PaymentRequest(
    val amount: Money,
    val type: PaymentType = PaymentType.SALE,
    val requestId: String = System.currentTimeMillis().toString(),
    val reference: String? = null,
    val allowPartialApproval: Boolean = false,
    val tipAmount: Money? = null
)
