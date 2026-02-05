package com.example.ccvpayment.model

/**
 * Printer status codes for the terminal's built-in printer.
 *
 * These statuses indicate the current state of the receipt printer
 * on the CCV payment terminal.
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
enum class PrinterStatus {
    /** Printer is ready and available for printing */
    AVAILABLE,

    /** Printer is not available or not connected */
    UNAVAILABLE,

    /** Printer paper roll is empty - needs replacement */
    PAPER_EMPTY,

    /** Printer paper is running low - should be replaced soon */
    PAPER_LOW
}
