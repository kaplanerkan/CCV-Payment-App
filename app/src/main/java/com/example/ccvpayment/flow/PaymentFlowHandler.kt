package com.example.ccvpayment.flow

import eu.ccvlab.mapi.core.payment.Money
import eu.ccvlab.mapi.core.payment.Payment
import eu.ccvlab.mapi.core.terminal.ExternalTerminal

/**
 * PaymentFlowHandler - Interface for handling payment flows.
 *
 * This interface abstracts the payment operations from the SDK.
 * Different implementations can be created for different OPI protocols
 * (OPI-DE, OPI-NL, OPI-CH).
 *
 * @author Erkan Kaplan
 * @since 1.0
 */
interface PaymentFlowHandler {

    // ==================== PAYMENT OPERATIONS ====================

    /**
     * Start a payment transaction.
     */
    fun startPayment(terminal: ExternalTerminal, payment: Payment)

    /**
     * Start a refund transaction.
     */
    fun startRefund(terminal: ExternalTerminal, type: Payment.Type, money: Money)

    /**
     * Start a void/reversal transaction.
     */
    fun startVoid(terminal: ExternalTerminal, type: Payment.Type, transactionId: String?)

    /**
     * Abort the current operation.
     */
    fun startAbort(terminal: ExternalTerminal)

    // ==================== TERMINAL OPERATIONS ====================

    /**
     * Get terminal status.
     */
    fun startStatus(terminal: ExternalTerminal)

    /**
     * Retrieve the last ticket/receipt.
     */
    fun startRetrieveLastTicket(terminal: ExternalTerminal)

    /**
     * Repeat the last message.
     */
    fun startRepeatLastMessage(terminal: ExternalTerminal)

    // ==================== REPORT OPERATIONS ====================

    /**
     * Perform period closing (Z-Report).
     */
    fun performPeriodClosing(terminal: ExternalTerminal)

    /**
     * Get transaction overview (X-Report).
     */
    fun getTransactionOverview(terminal: ExternalTerminal)

    // ==================== PRE-AUTHORIZATION ====================

    /**
     * Start a pre-authorization (reservation).
     */
    fun startReservation(terminal: ExternalTerminal, type: Payment.Type, money: Money)

    /**
     * Capture a pre-authorization.
     */
    fun startCapture(terminal: ExternalTerminal, money: Money, approvalCode: String?, token: String?)
}
