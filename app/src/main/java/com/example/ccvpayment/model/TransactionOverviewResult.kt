package com.example.ccvpayment.model

/**
 * Result of a transaction overview/history request.
 *
 * Contains a list of transactions processed during a specific
 * shift or time period.
 *
 * @property success Whether the request was successful
 * @property shiftNumber The shift number for the transactions
 * @property transactions List of transaction details
 * @property errorMessage Error description if request failed
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class TransactionOverviewResult(
    val success: Boolean,
    val shiftNumber: Int?,
    val transactions: List<TransactionInfo>,
    val errorMessage: String? = null
)
