package com.example.ccvpayment.helper

import eu.ccvlab.mapi.core.terminal.ExternalTerminal
import com.example.ccvpayment.model.*
import java.math.BigDecimal
import java.util.UUID

/**
 * CCV Payment Manager
 *
 * Tüm CCV ödeme işlemlerini yöneten ana facade sınıfı.
 * TerminalHelper, PaymentHelper ve TransactionHelper'ı birleştirir.
 */
class CCVPaymentManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: CCVPaymentManager? = null

        fun getInstance(): CCVPaymentManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CCVPaymentManager().also { INSTANCE = it }
            }
        }
    }

    private val terminalHelper = TerminalHelper.getInstance()
    private val paymentHelper = PaymentHelper.getInstance()
    private val transactionHelper = TransactionHelper.getInstance()

    private var currentTerminal: ExternalTerminal? = null

    // ==================== TERMINAL İŞLEMLERİ ====================

    /**
     * Yerel terminal bağlantısı al (Android terminal - 127.0.0.1:20002)
     */
    fun getLocalTerminal(): ExternalTerminal {
        val terminal = terminalHelper.getLocalTerminal()
        currentTerminal = terminal
        return terminal
    }

    /**
     * Özel IP ve port ile terminal oluştur
     */
    fun createTerminal(ipAddress: String, port: Int = 20002): ExternalTerminal {
        val terminal = terminalHelper.createTerminal(ipAddress, port)
        currentTerminal = terminal
        return terminal
    }

    /**
     * Aktif terminal'i döndür
     */
    fun getCurrentTerminal(): ExternalTerminal? = currentTerminal

    /**
     * Terminal durumunu kontrol et
     */
    fun checkTerminalStatus(
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (TerminalStatus?) -> Unit
    ) {
        terminalHelper.getStatus(terminal, object : TerminalHelper.TerminalStatusCallback {
            override fun onStatusReceived(status: TerminalStatus) {
                callback(status)
            }

            override fun onError(error: String) {
                callback(null)
            }
        })
    }

    /**
     * Terminal durumunu kontrol et (Coroutine)
     */
    suspend fun checkTerminalStatusAsync(
        terminal: ExternalTerminal = getLocalTerminal()
    ): TerminalStatus? {
        return try {
            terminalHelper.getStatusSuspend(terminal).getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Terminal başlat
     */
    fun startupTerminal(
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (Boolean, String?) -> Unit
    ) {
        terminalHelper.startup(terminal, object : TerminalHelper.TerminalOperationCallback {
            override fun onSuccess(message: String?) {
                callback(true, null)
            }

            override fun onError(error: String) {
                callback(false, error)
            }
        })
    }

    // ==================== ÖDEME İŞLEMLERİ ====================

    /**
     * Ödeme al (SALE)
     */
    fun makePayment(
        amount: BigDecimal,
        currency: String = "EUR",
        reference: String? = null,
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (PaymentResult) -> Unit
    ) {
        val request = PaymentRequest(
            amount = Money(amount, java.util.Currency.getInstance(currency)),
            type = PaymentType.SALE,
            requestId = UUID.randomUUID().toString(),
            reference = reference
        )

        paymentHelper.makePayment(terminal, request, object : PaymentHelper.PaymentCallback {
            override fun onSuccess(result: PaymentResult) {
                callback(result)
            }

            override fun onError(errorCode: String, errorMessage: String) {
                callback(PaymentResult(
                    status = TransactionStatus.FAILED,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                ))
            }
        })
    }

    /**
     * Ödeme al (Coroutine)
     */
    suspend fun makePaymentAsync(
        amount: BigDecimal,
        currency: String = "EUR",
        reference: String? = null,
        terminal: ExternalTerminal = getLocalTerminal()
    ): PaymentResult {
        val request = PaymentRequest(
            amount = Money(amount, java.util.Currency.getInstance(currency)),
            type = PaymentType.SALE,
            requestId = UUID.randomUUID().toString(),
            reference = reference
        )

        return paymentHelper.makePaymentSuspend(terminal, request).getOrElse {
            PaymentResult(
                status = TransactionStatus.FAILED,
                errorMessage = it.message
            )
        }
    }

    /**
     * İade işlemi (REFUND)
     */
    fun refund(
        amount: BigDecimal,
        originalTransactionId: String? = null,
        reason: String? = null,
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (PaymentResult) -> Unit
    ) {
        val request = RefundRequest(
            amount = Money(amount),
            originalTransactionId = originalTransactionId,
            reason = reason
        )

        paymentHelper.refund(terminal, request, object : PaymentHelper.PaymentCallback {
            override fun onSuccess(result: PaymentResult) {
                callback(result)
            }

            override fun onError(errorCode: String, errorMessage: String) {
                callback(PaymentResult(
                    status = TransactionStatus.FAILED,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                ))
            }
        })
    }

    /**
     * İade işlemi (Coroutine)
     */
    suspend fun refundAsync(
        amount: BigDecimal,
        originalTransactionId: String? = null,
        reason: String? = null,
        terminal: ExternalTerminal = getLocalTerminal()
    ): PaymentResult {
        val request = RefundRequest(
            amount = Money(amount),
            originalTransactionId = originalTransactionId,
            reason = reason
        )

        return paymentHelper.refundSuspend(terminal, request).getOrElse {
            PaymentResult(
                status = TransactionStatus.FAILED,
                errorMessage = it.message
            )
        }
    }

    /**
     * İşlem iptali (REVERSAL)
     */
    fun reversal(
        originalRequestId: String,
        amount: BigDecimal? = null,
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (PaymentResult) -> Unit
    ) {
        val request = ReversalRequest(
            originalRequestId = originalRequestId,
            amount = amount?.let { Money(it) }
        )

        paymentHelper.reversal(terminal, request, object : PaymentHelper.PaymentCallback {
            override fun onSuccess(result: PaymentResult) {
                callback(result)
            }

            override fun onError(errorCode: String, errorMessage: String) {
                callback(PaymentResult(
                    status = TransactionStatus.FAILED,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                ))
            }
        })
    }

    /**
     * Ön provizyon (AUTHORISE)
     */
    fun authorise(
        amount: BigDecimal,
        currency: String = "EUR",
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (PaymentResult) -> Unit
    ) {
        val request = PaymentRequest(
            amount = Money(amount, java.util.Currency.getInstance(currency)),
            type = PaymentType.AUTHORISE,
            requestId = UUID.randomUUID().toString()
        )

        paymentHelper.authorise(terminal, request, object : PaymentHelper.PaymentCallback {
            override fun onSuccess(result: PaymentResult) {
                callback(result)
            }

            override fun onError(errorCode: String, errorMessage: String) {
                callback(PaymentResult(
                    status = TransactionStatus.FAILED,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                ))
            }
        })
    }

    /**
     * Ön provizyonu tamamla (CAPTURE)
     */
    fun capture(
        originalRequestId: String,
        amount: BigDecimal,
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (PaymentResult) -> Unit
    ) {
        paymentHelper.capture(terminal, originalRequestId, Money(amount), object : PaymentHelper.PaymentCallback {
            override fun onSuccess(result: PaymentResult) {
                callback(result)
            }

            override fun onError(errorCode: String, errorMessage: String) {
                callback(PaymentResult(
                    status = TransactionStatus.FAILED,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                ))
            }
        })
    }

    /**
     * Ödemeyi durdur
     */
    fun stopPayment(
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (Boolean) -> Unit
    ) {
        paymentHelper.stopPayment(terminal, object : PaymentHelper.PaymentCallback {
            override fun onSuccess(result: PaymentResult) {
                callback(true)
            }

            override fun onError(errorCode: String, errorMessage: String) {
                callback(false)
            }
        })
    }

    /**
     * İşlem kurtarma (bağlantı kopması durumunda)
     */
    fun recoverPayment(
        originalRequestId: String,
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (PaymentResult) -> Unit
    ) {
        paymentHelper.recoverPayment(terminal, originalRequestId, object : PaymentHelper.PaymentCallback {
            override fun onSuccess(result: PaymentResult) {
                callback(result)
            }

            override fun onError(errorCode: String, errorMessage: String) {
                callback(PaymentResult(
                    status = TransactionStatus.FAILED,
                    errorCode = errorCode,
                    errorMessage = errorMessage
                ))
            }
        })
    }

    // ==================== RAPOR İŞLEMLERİ ====================

    /**
     * Gün Sonu (Z-Report) - Vardiyayı kapatır
     */
    fun periodClosing(
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (PeriodClosingResult) -> Unit
    ) {
        transactionHelper.periodClosing(terminal, object : TransactionHelper.PeriodClosingCallback {
            override fun onSuccess(result: PeriodClosingResult) {
                callback(result)
            }

            override fun onError(error: String) {
                callback(PeriodClosingResult(
                    success = false,
                    shiftNumber = null,
                    totalTransactions = 0,
                    totalSalesCount = 0,
                    totalSalesAmount = null,
                    totalRefundsCount = 0,
                    totalRefundsAmount = null,
                    netAmount = null,
                    receipt = null,
                    errorMessage = error
                ))
            }
        })
    }

    /**
     * Gün Sonu (Coroutine)
     */
    suspend fun periodClosingAsync(
        terminal: ExternalTerminal = getLocalTerminal()
    ): PeriodClosingResult {
        return transactionHelper.periodClosingSuspend(terminal).getOrElse {
            PeriodClosingResult(
                success = false,
                shiftNumber = null,
                totalTransactions = 0,
                totalSalesCount = 0,
                totalSalesAmount = null,
                totalRefundsCount = 0,
                totalRefundsAmount = null,
                netAmount = null,
                receipt = null,
                errorMessage = it.message
            )
        }
    }

    /**
     * X-Report - Vardiyayı kapatmadan rapor al
     */
    fun xReport(
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (PeriodClosingResult) -> Unit
    ) {
        transactionHelper.partialPeriodClosing(terminal, object : TransactionHelper.PeriodClosingCallback {
            override fun onSuccess(result: PeriodClosingResult) {
                callback(result)
            }

            override fun onError(error: String) {
                callback(PeriodClosingResult(
                    success = false,
                    shiftNumber = null,
                    totalTransactions = 0,
                    totalSalesCount = 0,
                    totalSalesAmount = null,
                    totalRefundsCount = 0,
                    totalRefundsAmount = null,
                    netAmount = null,
                    receipt = null,
                    errorMessage = error
                ))
            }
        })
    }

    /**
     * X-Report (Coroutine)
     */
    suspend fun xReportAsync(
        terminal: ExternalTerminal = getLocalTerminal()
    ): PeriodClosingResult {
        return transactionHelper.partialPeriodClosingSuspend(terminal).getOrElse {
            PeriodClosingResult(
                success = false,
                shiftNumber = null,
                totalTransactions = 0,
                totalSalesCount = 0,
                totalSalesAmount = null,
                totalRefundsCount = 0,
                totalRefundsAmount = null,
                netAmount = null,
                receipt = null,
                errorMessage = it.message
            )
        }
    }

    /**
     * İşlem geçmişini al
     */
    fun getTransactionHistory(
        shiftNumber: Int? = null,
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (TransactionOverviewResult) -> Unit
    ) {
        transactionHelper.getTransactionOverview(terminal, shiftNumber,
            object : TransactionHelper.TransactionOverviewCallback {
                override fun onSuccess(result: TransactionOverviewResult) {
                    callback(result)
                }

                override fun onError(error: String) {
                    callback(TransactionOverviewResult(
                        success = false,
                        shiftNumber = null,
                        transactions = emptyList(),
                        errorMessage = error
                    ))
                }
            })
    }

    /**
     * İşlem geçmişini al (Coroutine)
     */
    suspend fun getTransactionHistoryAsync(
        shiftNumber: Int? = null,
        terminal: ExternalTerminal = getLocalTerminal()
    ): TransactionOverviewResult {
        return transactionHelper.getTransactionOverviewSuspend(terminal, shiftNumber).getOrElse {
            TransactionOverviewResult(
                success = false,
                shiftNumber = null,
                transactions = emptyList(),
                errorMessage = it.message
            )
        }
    }

    /**
     * Son fişi tekrar yazdır
     */
    fun reprintLastTicket(
        terminal: ExternalTerminal = getLocalTerminal(),
        onResult: (merchantReceipt: String?, customerReceipt: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        transactionHelper.reprintLastTicket(terminal, object : TransactionHelper.LastTicketCallback {
            override fun onSuccess(merchantReceipt: String?, customerReceipt: String?) {
                onResult(merchantReceipt, customerReceipt)
            }

            override fun onError(error: String) {
                onError(error)
            }
        })
    }

    /**
     * Son fişi al (Coroutine)
     */
    suspend fun reprintLastTicketAsync(
        terminal: ExternalTerminal = getLocalTerminal()
    ): Pair<String?, String?> {
        return transactionHelper.reprintLastTicketSuspend(terminal).getOrElse {
            Pair(null, null)
        }
    }

    /**
     * Açık ön provizyonları getir
     */
    fun getOpenPreAuthorisations(
        terminal: ExternalTerminal = getLocalTerminal(),
        callback: (TransactionOverviewResult) -> Unit
    ) {
        transactionHelper.getOpenPreAuthorisations(terminal,
            object : TransactionHelper.TransactionOverviewCallback {
                override fun onSuccess(result: TransactionOverviewResult) {
                    callback(result)
                }

                override fun onError(error: String) {
                    callback(TransactionOverviewResult(
                        success = false,
                        shiftNumber = null,
                        transactions = emptyList(),
                        errorMessage = error
                    ))
                }
            })
    }
}
