package com.example.ccvpayment.model

/**
 * Terminal communication protocol types.
 *
 * CCV terminals support different OPI (Open Payment Initiative) protocols
 * depending on the region and terminal configuration.
 *
 * @since 1.0
 * @author Erkan Kaplan
 * @date 2026-02-05
 */
enum class TerminalProtocol {
    /** OPI-NL protocol - Used primarily in the Netherlands */
    OPI_NL,

    /** OPI-DE protocol - Used primarily in Germany */
    OPI_DE
}
