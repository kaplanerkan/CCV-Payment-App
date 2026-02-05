package com.example.ccvpayment.model

/**
 * Socket connection mode for terminal communication.
 *
 * Defines how the application communicates with the CCV terminal
 * over the network connection.
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
enum class SocketMode {
    /**
     * Single socket mode - Uses one socket for all communication.
     * Simpler but may have limitations with concurrent operations.
     */
    SINGLE_SOCKET,

    /**
     * Dual socket mode - Uses separate sockets for commands and responses.
     * Recommended for production use with better reliability.
     */
    DUAL_SOCKET
}
