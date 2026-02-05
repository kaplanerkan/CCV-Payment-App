package com.example.ccvpayment.model

/**
 * Receipt handling mode during payment transactions.
 *
 * Defines when and how receipts are generated and returned
 * during the payment process.
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
enum class ReceiptMode {
    /** Default mode - receipts are printed during payment */
    DEFAULT,

    /** No receipts - receipts are not generated */
    OFF,

    /** Receipts are included in the payment response for app handling */
    RECEIPTS_IN_RESPONSE
}
