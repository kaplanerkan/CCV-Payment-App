package com.example.ccvpayment.flow

import eu.ccvlab.mapi.api.PaymentService
import eu.ccvlab.mapi.api.TerminalService
import eu.ccvlab.mapi.core.api.PaymentApi
import eu.ccvlab.mapi.core.api.TerminalApi
import eu.ccvlab.mapi.core.payment.Money
import eu.ccvlab.mapi.core.payment.Payment
import eu.ccvlab.mapi.core.terminal.ExternalTerminal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OpiDeFlowHandler - Flow handler for OPI-DE protocol.
 *
 * This class implements the PaymentFlowHandler interface for
 * the OPI-DE (German) protocol used by CCV terminals.
 *
 * Port: 30002
 * Socket Mode: SINGLE_SOCKET
 *
 * @author Erkan Kaplan
 * @since 1.0
 */
class OpiDeFlowHandler(
    private val delegationFactory: DelegationFactory
) : PaymentFlowHandler {

    private val paymentService: PaymentApi = PaymentService()
    private val terminalService: TerminalApi = TerminalService()
    private val posTimestampFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.getDefault())

    // ==================== PAYMENT OPERATIONS ====================

    override fun startPayment(terminal: ExternalTerminal, payment: Payment) {
        val delegate = delegationFactory.createPaymentDelegate(Flow.PAYMENT.description)
        paymentService.payment(terminal, payment, delegate)
    }

    override fun startRefund(terminal: ExternalTerminal, type: Payment.Type, money: Money) {
        val payment = Payment.builder()
            .type(type)
            .amount(money)
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        val delegate = delegationFactory.createPaymentDelegate(Flow.REFUND.description)
        paymentService.payment(terminal, payment, delegate)
    }

    override fun startVoid(terminal: ExternalTerminal, type: Payment.Type, transactionId: String?) {
        val paymentBuilder = Payment.builder()
            .type(type)
            .posTimestamp(posTimestampFormatter.format(Date()))

        transactionId?.let { paymentBuilder.transactionId(it) }

        val payment = paymentBuilder.build()
        val delegate = delegationFactory.createPaymentDelegate(Flow.VOID.description)
        paymentService.payment(terminal, payment, delegate)
    }

    override fun startAbort(terminal: ExternalTerminal) {
        val delegate = delegationFactory.createTerminalDelegate(Flow.ABORT.description)
        paymentService.abort(terminal, delegate)
    }

    // ==================== TERMINAL OPERATIONS ====================

    override fun startStatus(terminal: ExternalTerminal) {
        val delegate = delegationFactory.createTerminalDelegate(Flow.STATUS.description)
        terminalService.getStatus(terminal, delegate)
    }

    override fun startRetrieveLastTicket(terminal: ExternalTerminal) {
        val delegate = delegationFactory.createTerminalDelegate(Flow.RETRIEVE_LAST_TICKET.description)
        terminalService.retrieveLastTicket(terminal, delegate)
    }

    override fun startRepeatLastMessage(terminal: ExternalTerminal) {
        val delegate = delegationFactory.createTerminalDelegate(Flow.REPEAT_LAST_MESSAGE.description)
        terminalService.repeatLastMessage(terminal, delegate)
    }

    // ==================== REPORT OPERATIONS ====================

    override fun performPeriodClosing(terminal: ExternalTerminal) {
        val delegate = delegationFactory.createTerminalDelegate(Flow.PERIOD_CLOSING.description)
        terminalService.periodClosing(terminal, delegate)
    }

    override fun getTransactionOverview(terminal: ExternalTerminal) {
        val delegate = delegationFactory.createTerminalDelegate(Flow.TRANSACTION_OVERVIEW.description)
        terminalService.transactionOverview(terminal, delegate)
    }

    // ==================== PRE-AUTHORIZATION ====================

    override fun startReservation(terminal: ExternalTerminal, type: Payment.Type, money: Money) {
        val payment = Payment.builder()
            .type(Payment.Type.RESERVATION)
            .amount(money)
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        val delegate = delegationFactory.createPaymentDelegate(Flow.AUTHORISE.description)
        paymentService.reservation(terminal, payment, delegate)
    }

    override fun startCapture(terminal: ExternalTerminal, money: Money, approvalCode: String?, token: String?) {
        val payment = Payment.builder()
            .type(Payment.Type.SALE)
            .amount(money)
            .approvalCode(approvalCode)
            .token(token)
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        val delegate = delegationFactory.createPaymentDelegate(Flow.CAPTURE.description)
        paymentService.payment(terminal, payment, delegate)
    }
}
