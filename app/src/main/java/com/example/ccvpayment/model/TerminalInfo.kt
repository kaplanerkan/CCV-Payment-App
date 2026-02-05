package com.example.ccvpayment.model

/**
 * Configuration information for a CCV payment terminal.
 *
 * Contains all the network and protocol settings needed to
 * establish communication with a specific terminal.
 *
 * @property ipAddress The IP address of the terminal
 * @property port The main communication port
 * @property compatibilityPort Secondary port for dual-socket mode
 * @property protocol The OPI protocol variant in use
 * @property socketMode Single or dual socket communication mode
 * @property terminalId The unique identifier of the terminal
 * @property softwareVersion The terminal's software version
 * @property isConnected Whether the terminal is currently connected
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
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
