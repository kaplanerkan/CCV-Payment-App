package com.example.ccvpayment.helper

import android.util.Log
import com.example.ccvpayment.BuildConfig
import com.example.ccvpayment.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import eu.ccvlab.mapi.core.payment.Payment
import eu.ccvlab.mapi.core.payment.PaymentResult as MapiPaymentResult
import eu.ccvlab.mapi.core.payment.PaymentAdministrationResult
import eu.ccvlab.mapi.core.terminal.ExternalTerminal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CCV Logger - Centralized logging for all CCV payment operations.
 *
 * This singleton class provides comprehensive logging capabilities for
 * debugging and monitoring payment transactions. All request and response
 * data is logged to Logcat in a formatted JSON structure.
 *
 * Features:
 * - Request logging (outgoing data)
 * - Response logging (incoming data)
 * - Error logging
 * - Terminal event logging
 * - Pretty-printed JSON output
 * - Timestamp for each log entry
 *
 * Log Tags:
 * - CCV_REQUEST: Outgoing requests
 * - CCV_RESPONSE: Incoming responses
 * - CCV_ERROR: Error messages
 * - CCV_EVENT: Terminal events and callbacks
 *
 * @author Erkan Kaplan
 * @date 2026-02-05
 * @since 1.0
 */
object CCVLogger {

    private const val TAG_REQUEST = "CCV_REQUEST"
    private const val TAG_RESPONSE = "CCV_RESPONSE"
    private const val TAG_ERROR = "CCV_ERROR"
    private const val TAG_EVENT = "CCV_EVENT"
    private const val TAG_DEBUG = "CCV_DEBUG"

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    private val timestampFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    /** Enable/disable logging (can be controlled at runtime) */
    var isEnabled: Boolean = BuildConfig.DEBUG

    /** Enable/disable verbose logging (includes full JSON) */
    var isVerbose: Boolean = true

    // ==================== REQUEST LOGGING ====================

    /**
     * Log a payment request (SALE, REFUND, etc.)
     */
    fun logPaymentRequest(
        operation: String,
        terminal: ExternalTerminal,
        request: PaymentRequest
    ) {
        if (!isEnabled) return

        val logData = mapOf(
            "timestamp" to timestamp(),
            "operation" to operation,
            "terminal" to terminalInfo(terminal),
            "request" to mapOf(
                "requestId" to request.requestId,
                "type" to request.type.name,
                "amount" to request.amount.formatted(),
                "reference" to request.reference,
                "allowPartialApproval" to request.allowPartialApproval,
                "tipAmount" to request.tipAmount?.formatted()
            )
        )

        log(TAG_REQUEST, ">>> $operation REQUEST", logData)
    }

    /**
     * Log a refund request
     */
    fun logRefundRequest(
        terminal: ExternalTerminal,
        request: RefundRequest
    ) {
        if (!isEnabled) return

        val logData = mapOf(
            "timestamp" to timestamp(),
            "operation" to "REFUND",
            "terminal" to terminalInfo(terminal),
            "request" to mapOf(
                "requestId" to request.requestId,
                "amount" to request.amount.formatted(),
                "originalTransactionId" to request.originalTransactionId,
                "reason" to request.reason
            )
        )

        log(TAG_REQUEST, ">>> REFUND REQUEST", logData)
    }

    /**
     * Log a reversal/void request
     */
    fun logReversalRequest(
        terminal: ExternalTerminal,
        request: ReversalRequest
    ) {
        if (!isEnabled) return

        val logData = mapOf(
            "timestamp" to timestamp(),
            "operation" to "REVERSAL",
            "terminal" to terminalInfo(terminal),
            "request" to mapOf(
                "originalRequestId" to request.originalRequestId,
                "amount" to request.amount?.formatted()
            )
        )

        log(TAG_REQUEST, ">>> REVERSAL REQUEST", logData)
    }

    /**
     * Log a terminal operation request
     */
    fun logTerminalRequest(
        operation: String,
        terminal: ExternalTerminal,
        additionalData: Map<String, Any?>? = null
    ) {
        if (!isEnabled) return

        val logData = mutableMapOf<String, Any?>(
            "timestamp" to timestamp(),
            "operation" to operation,
            "terminal" to terminalInfo(terminal)
        )

        additionalData?.let { logData["data"] = it }

        log(TAG_REQUEST, ">>> $operation REQUEST", logData)
    }

    // ==================== RESPONSE LOGGING ====================

    /**
     * Log a payment result (success)
     */
    fun logPaymentResponse(
        operation: String,
        result: PaymentResult
    ) {
        if (!isEnabled) return

        val logData = mapOf(
            "timestamp" to timestamp(),
            "operation" to operation,
            "status" to result.status.name,
            "result" to mapOf(
                "transactionId" to result.transactionId,
                "requestId" to result.requestId,
                "amount" to result.amount?.formatted(),
                "approvedAmount" to result.approvedAmount?.formatted(),
                "cardBrand" to result.cardBrand,
                "maskedPan" to result.maskedPan,
                "authCode" to result.authCode,
                "isPartialApproval" to result.isPartialApproval,
                "timestamp" to result.timestamp
            )
        )

        log(TAG_RESPONSE, "<<< $operation RESPONSE [${result.status}]", logData)
    }

    /**
     * Log a terminal status response
     */
    fun logTerminalStatusResponse(status: TerminalStatus) {
        if (!isEnabled) return

        val logData = mapOf(
            "timestamp" to timestamp(),
            "operation" to "TERMINAL_STATUS",
            "status" to mapOf(
                "isConnected" to status.isConnected,
                "terminalId" to status.terminalId,
                "softwareVersion" to status.softwareVersion,
                "ipAddress" to status.ipAddress,
                "batteryLevel" to status.batteryLevel,
                "printerStatus" to status.printerStatus?.name
            )
        )

        log(TAG_RESPONSE, "<<< TERMINAL STATUS [connected=${status.isConnected}]", logData)
    }

    /**
     * Log a period closing result (Z-Report / X-Report)
     */
    fun logPeriodClosingResponse(
        isZReport: Boolean,
        result: PeriodClosingResult
    ) {
        if (!isEnabled) return

        val reportType = if (isZReport) "Z_REPORT" else "X_REPORT"

        val logData = mapOf(
            "timestamp" to timestamp(),
            "operation" to reportType,
            "success" to result.success,
            "result" to mapOf(
                "shiftNumber" to result.shiftNumber,
                "totalTransactions" to result.totalTransactions,
                "totalSalesCount" to result.totalSalesCount,
                "totalSalesAmount" to result.totalSalesAmount?.formatted(),
                "totalRefundsCount" to result.totalRefundsCount,
                "totalRefundsAmount" to result.totalRefundsAmount?.formatted(),
                "netAmount" to result.netAmount?.formatted(),
                "closingTime" to result.closingTime
            )
        )

        log(TAG_RESPONSE, "<<< $reportType RESPONSE [success=${result.success}]", logData)
    }

    /**
     * Log transaction overview response
     */
    fun logTransactionOverviewResponse(result: TransactionOverviewResult) {
        if (!isEnabled) return

        val logData = mapOf(
            "timestamp" to timestamp(),
            "operation" to "TRANSACTION_OVERVIEW",
            "success" to result.success,
            "shiftNumber" to result.shiftNumber,
            "transactionCount" to result.transactions.size,
            "transactions" to result.transactions.map { tx ->
                mapOf(
                    "transactionId" to tx.transactionId,
                    "type" to tx.type.name,
                    "status" to tx.status.name,
                    "amount" to tx.amount.formatted(),
                    "cardBrand" to tx.cardBrand,
                    "timestamp" to tx.timestamp
                )
            }
        )

        log(TAG_RESPONSE, "<<< TRANSACTION OVERVIEW [count=${result.transactions.size}]", logData)
    }

    // ==================== ERROR LOGGING ====================

    /**
     * Log an error
     */
    fun logError(
        operation: String,
        errorCode: String?,
        errorMessage: String?,
        exception: Throwable? = null
    ) {
        if (!isEnabled) return

        val logData = mapOf(
            "timestamp" to timestamp(),
            "operation" to operation,
            "error" to mapOf(
                "code" to errorCode,
                "message" to errorMessage,
                "exception" to exception?.message,
                "stackTrace" to exception?.stackTraceToString()?.take(500)
            )
        )

        Log.e(TAG_ERROR, "!!! $operation ERROR: $errorMessage")
        if (isVerbose) {
            Log.e(TAG_ERROR, gson.toJson(logData))
        }
    }

    // ==================== EVENT LOGGING ====================

    /**
     * Log a terminal event (display message, signature request, etc.)
     */
    fun logEvent(event: String, data: Any? = null) {
        if (!isEnabled) return

        val logData = mapOf(
            "timestamp" to timestamp(),
            "event" to event,
            "data" to data
        )

        Log.i(TAG_EVENT, "--- EVENT: $event")
        if (isVerbose && data != null) {
            Log.i(TAG_EVENT, gson.toJson(logData))
        }
    }

    /**
     * Log a debug message
     */
    fun debug(tag: String, message: String, data: Any? = null) {
        if (!isEnabled) return

        Log.d(TAG_DEBUG, "[$tag] $message")
        if (isVerbose && data != null) {
            Log.d(TAG_DEBUG, gson.toJson(data))
        }
    }

    // ==================== RAW SDK LOGGING ====================

    /**
     * Log raw SDK Payment object (outgoing)
     */
    fun logRawPayment(payment: Payment) {
        if (!isEnabled || !isVerbose) return

        Log.d(TAG_REQUEST, "=== RAW SDK Payment ===")
        Log.d(TAG_REQUEST, "Type: ${payment.type()}")
        Log.d(TAG_REQUEST, "Amount: ${payment.amount()}")
        Log.d(TAG_REQUEST, "RequestId: ${payment.requestId()}")
        Log.d(TAG_REQUEST, "PosTimestamp: ${payment.posTimestamp()}")
    }

    /**
     * Log raw SDK PaymentResult object (incoming)
     */
    fun logRawPaymentResult(result: MapiPaymentResult?) {
        if (!isEnabled || !isVerbose || result == null) return

        Log.d(TAG_RESPONSE, "=== RAW SDK PaymentResult ===")
        Log.d(TAG_RESPONSE, "ApprovalCode: ${result.approvalCode()}")
        Log.d(TAG_RESPONSE, "Amount: ${result.amount()}")
        Log.d(TAG_RESPONSE, "Token: ${result.token()}")
        Log.d(TAG_RESPONSE, "HashData: ${result.hashData()}")
    }

    /**
     * Log raw SDK PaymentAdministrationResult object (incoming)
     */
    fun logRawAdminResult(result: PaymentAdministrationResult<*>?) {
        if (!isEnabled || !isVerbose || result == null) return

        Log.d(TAG_RESPONSE, "=== RAW SDK PaymentAdministrationResult ===")
        Log.d(TAG_RESPONSE, result.toString())
    }

    // ==================== HELPER METHODS ====================

    private fun timestamp(): String = timestampFormatter.format(Date())

    private fun terminalInfo(terminal: ExternalTerminal): Map<String, Any?> {
        return mapOf(
            "ipAddress" to terminal.ipAddress(),
            "port" to terminal.port(),
            "socketMode" to terminal.socketMode()?.name,
            "terminalType" to terminal.terminalType()?.name
        )
    }

    private fun log(tag: String, header: String, data: Map<String, Any?>) {
        Log.i(tag, "════════════════════════════════════════")
        Log.i(tag, header)
        Log.i(tag, "════════════════════════════════════════")

        if (isVerbose) {
            try {
                val json = gson.toJson(data)
                // Split long JSON into chunks for Logcat (max ~4000 chars per line)
                json.chunked(3000).forEach { chunk ->
                    Log.i(tag, chunk)
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to serialize log data: ${e.message}")
            }
        }
    }

    /**
     * Log separator for better visibility in Logcat
     */
    fun logSeparator(label: String = "") {
        if (!isEnabled) return
        Log.i(TAG_DEBUG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        if (label.isNotEmpty()) {
            Log.i(TAG_DEBUG, "  $label")
            Log.i(TAG_DEBUG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
}
