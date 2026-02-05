package com.example.ccvpayment.model

/**
 * Payment transaction types supported by the CCV terminal.
 *
 * This enum defines all the payment operation types that can be performed
 * through the CCV mAPI SDK.
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
enum class PaymentType {
    /** Standard sale transaction - charges the customer's card */
    SALE,

    /** Refund transaction - returns money to the customer's card */
    REFUND,

    /** Reversal/void transaction - cancels a previous transaction */
    REVERSAL,

    /** Pre-authorization - reserves an amount on the customer's card */
    AUTHORISE,

    /** Capture - completes a previously authorized transaction */
    CAPTURE
}
