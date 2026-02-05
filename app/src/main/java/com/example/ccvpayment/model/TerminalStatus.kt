package com.example.ccvpayment.model

import java.util.Date

/**
 * Terminal Durum Bilgisi
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
