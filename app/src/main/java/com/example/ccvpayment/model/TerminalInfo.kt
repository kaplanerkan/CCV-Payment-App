package com.example.ccvpayment.model

/**
 * Terminal Bilgisi
 */
data class TerminalInfo(
    val ipAddress: String,
    val port: Int,
    val compatibilityPort: Int,
    val protocol: TerminalProtocol,
    val socketMode: SocketMode,
    val terminalId: String? = null,
    val softwareVersion: String? = null,
    val isConnected: Boolean = false
)
