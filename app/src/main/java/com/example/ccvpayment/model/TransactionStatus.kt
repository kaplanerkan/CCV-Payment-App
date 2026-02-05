package com.example.ccvpayment.model

/**
 * Transaction status codes returned by the payment terminal.
 *
 * These statuses indicate the final outcome of a payment transaction
 * after it has been processed by the CCV terminal.
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
enum class TransactionStatus {
    /** Transaction completed successfully */
    SUCCESS,

    /** Transaction failed due to an error */
    FAILED,

    /** Transaction was cancelled by user or merchant */
    CANCELLED,

    /** Transaction is still being processed */
    PENDING,

    /** Transaction timed out waiting for response */
    TIMEOUT,

    /** Transaction status could not be determined */
    UNKNOWN
}
