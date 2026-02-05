package com.example.ccvpayment.model

import java.util.Date

/**
 * Current status information of a CCV payment terminal.
 *
 * Contains real-time status data retrieved from the terminal
 * including connection state, battery level, and printer status.
 *
 * @property isConnected Whether the terminal is currently connected and responsive
 * @property terminalId The unique identifier of the terminal
 * @property softwareVersion The terminal's software version
 * @property ipAddress The current IP address of the terminal
 * @property batteryLevel Battery charge percentage (0-100) for mobile terminals
 * @property printerStatus Current status of the built-in receipt printer
 * @property lastTransactionTime Timestamp of the most recent transaction
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
data class TerminalStatus(
    val isConnected: Boolean,
    val terminalId: String? = null,
    val softwareVersion: String? = null,
    val ipAddress: String? = null,
    val batteryLevel: Int? = null,
    val printerStatus: PrinterStatus? = null,
    val lastTransactionTime: Date? = null
)
