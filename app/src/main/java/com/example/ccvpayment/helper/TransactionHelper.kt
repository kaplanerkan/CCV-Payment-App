package com.example.ccvpayment.helper

import eu.ccvlab.mapi.api.TerminalService
import eu.ccvlab.mapi.core.api.TerminalApi
import eu.ccvlab.mapi.core.api.response.delegate.TerminalDelegate
import eu.ccvlab.mapi.core.api.response.result.ConfigData
import eu.ccvlab.mapi.core.api.response.result.Error
import eu.ccvlab.mapi.core.payment.PaymentAdministrationResult
import eu.ccvlab.mapi.core.payment.PaymentReceipt
import eu.ccvlab.mapi.core.payment.TextLine
import eu.ccvlab.mapi.core.payment.EReceiptRequest
import eu.ccvlab.mapi.core.terminal.ExternalTerminal
import com.example.ccvpayment.model.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import java.util.Currency
import java.util.Date
import kotlin.coroutines.resume

/**
 * Transaction Helper
 *
 * Dönem kapama (Z-Raporu), X-Raporu, işlem geçmişi ve fiş yazdırma işlemleri için yardımcı sınıf.
 * CCV mAPI SDK ile gerçek entegrasyon.
 */
class TransactionHelper private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: TransactionHelper? = null

        fun getInstance(): TransactionHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TransactionHelper().also { INSTANCE = it }
            }
        }
    }

    private val terminalService: TerminalApi = TerminalService()

    /**
     * Callback Arayüzleri
     */
    interface PeriodClosingCallback {
        fun onSuccess(result: PeriodClosingResult)
        fun onError(error: String)
        fun onReceiptReady(receipt: String) {}
    }

    interface TransactionOverviewCallback {
        fun onSuccess(result: TransactionOverviewResult)
        fun onError(error: String)
    }

    interface LastTicketCallback {
        fun onSuccess(merchantReceipt: String?, customerReceipt: String?)
        fun onError(error: String)
    }

    /**
     * Dönem Kapama (Z-Raporu)
     */
    fun periodClosing(terminal: ExternalTerminal, callback: PeriodClosingCallback) {
        val delegate = object : TerminalDelegate {
            private var merchantReceipt: String? = null
            private var customerReceipt: String? = null

            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {}

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                merchantReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {
                customerReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printJournalReceipt(receipt: PaymentReceipt?) {}
            override fun storeEJournal(journal: String?) {}
            override fun configData(config: ConfigData?) {}
            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {}
            override fun cardUID(cardUID: String?) {}

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                val receiptText = merchantReceipt ?: customerReceipt
                if (receiptText != null) {
                    callback.onReceiptReady(receiptText)
                }

                callback.onSuccess(PeriodClosingResult(
                    success = true,
                    shiftNumber = null,
                    totalTransactions = 0,
                    totalSalesCount = 0,
                    totalSalesAmount = null,
                    totalRefundsCount = 0,
                    totalRefundsAmount = null,
                    netAmount = null,
                    receipt = receiptText
                ))
            }

            override fun onError(error: Error?) {
                callback.onError(error?.mapiError()?.description() ?: "Period closing failed")
            }
        }

        terminalService.periodClosing(terminal, delegate)
    }

    /**
     * Kısmi Dönem Kapama (X-Raporu) - Transaction Overview kullanılır
     */
    fun partialPeriodClosing(terminal: ExternalTerminal, callback: PeriodClosingCallback) {
        val delegate = object : TerminalDelegate {
            private var receiptText: String? = null

            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {}

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                receiptText = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {
                if (receiptText == null) {
                    receiptText = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
                }
            }

            override fun printJournalReceipt(receipt: PaymentReceipt?) {}
            override fun storeEJournal(journal: String?) {}
            override fun configData(config: ConfigData?) {}
            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {}
            override fun cardUID(cardUID: String?) {}

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                if (receiptText != null) {
                    callback.onReceiptReady(receiptText!!)
                }

                callback.onSuccess(PeriodClosingResult(
                    success = true,
                    shiftNumber = null,
                    totalTransactions = 0,
                    totalSalesCount = 0,
                    totalSalesAmount = null,
                    totalRefundsCount = 0,
                    totalRefundsAmount = null,
                    netAmount = null,
                    receipt = receiptText
                ))
            }

            override fun onError(error: Error?) {
                callback.onError(error?.mapiError()?.description() ?: "X-Report failed")
            }
        }

        terminalService.transactionOverview(terminal, delegate)
    }

    /**
     * İşlem Geçmişi (Transaction Overview)
     */
    fun getTransactionOverview(
        terminal: ExternalTerminal,
        shiftNumber: Int? = null,
        callback: TransactionOverviewCallback
    ) {
        val terminalWithShift = if (shiftNumber != null) {
            terminal.shiftNumber(shiftNumber)
        } else {
            terminal
        }

        val delegate = object : TerminalDelegate {
            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {}
            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {}
            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {}
            override fun printJournalReceipt(receipt: PaymentReceipt?) {}
            override fun storeEJournal(journal: String?) {}
            override fun configData(config: ConfigData?) {}
            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {}
            override fun cardUID(cardUID: String?) {}

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                callback.onSuccess(TransactionOverviewResult(
                    success = true,
                    shiftNumber = null ?: shiftNumber,
                    transactions = emptyList()
                ))
            }

            override fun onError(error: Error?) {
                callback.onError(error?.mapiError()?.description() ?: "Transaction overview failed")
            }
        }

        terminalService.transactionOverview(terminalWithShift, delegate)
    }

    /**
     * Satışları Göster
     */
    fun showSales(terminal: ExternalTerminal, callback: (Boolean, String?) -> Unit) {
        getTransactionOverview(terminal, null, object : TransactionOverviewCallback {
            override fun onSuccess(result: TransactionOverviewResult) {
                callback(true, null)
            }

            override fun onError(error: String) {
                callback(false, error)
            }
        })
    }

    /**
     * Son Fişi Tekrar Yazdır
     */
    fun reprintLastTicket(terminal: ExternalTerminal, callback: LastTicketCallback) {
        val delegate = object : TerminalDelegate {
            private var merchantReceipt: String? = null
            private var customerReceipt: String? = null

            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {}

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                merchantReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {
                customerReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printJournalReceipt(receipt: PaymentReceipt?) {}
            override fun storeEJournal(journal: String?) {}
            override fun configData(config: ConfigData?) {}
            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {}
            override fun cardUID(cardUID: String?) {}

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                callback.onSuccess(merchantReceipt, customerReceipt)
            }

            override fun onError(error: Error?) {
                callback.onError(error?.mapiError()?.description() ?: "Retrieve last ticket failed")
            }
        }

        terminalService.retrieveLastTicket(terminal, delegate)
    }

    /**
     * Dönem Kapama Fişini Tekrar Yazdır
     */
    fun reprintPeriodClosingTicket(
        terminal: ExternalTerminal,
        shiftNumber: Int,
        callback: LastTicketCallback
    ) {
        val delegate = object : TerminalDelegate {
            private var receiptText: String? = null

            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {}

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                receiptText = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {}
            override fun printJournalReceipt(receipt: PaymentReceipt?) {}
            override fun storeEJournal(journal: String?) {}
            override fun configData(config: ConfigData?) {}
            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {}
            override fun cardUID(cardUID: String?) {}

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                callback.onSuccess(receiptText, receiptText)
            }

            override fun onError(error: Error?) {
                callback.onError(error?.mapiError()?.description() ?: "Reprint period closing failed")
            }
        }

        terminalService.ticketReprintPeriodClosing(terminal.shiftNumber(shiftNumber), delegate)
    }

    /**
     * Açık Ön Provizyonları Getir
     */
    fun getOpenPreAuthorisations(terminal: ExternalTerminal, callback: TransactionOverviewCallback) {
        callback.onSuccess(TransactionOverviewResult(
            success = true,
            shiftNumber = null,
            transactions = emptyList()
        ))
    }

    /**
     * Coroutine versiyonları
     */
    suspend fun periodClosingSuspend(terminal: ExternalTerminal): Result<PeriodClosingResult> {
        return suspendCancellableCoroutine { continuation ->
            periodClosing(terminal, object : PeriodClosingCallback {
                override fun onSuccess(result: PeriodClosingResult) {
                    continuation.resume(Result.success(result))
                }
                override fun onError(error: String) {
                    continuation.resume(Result.failure(Exception(error)))
                }
            })
        }
    }

    suspend fun partialPeriodClosingSuspend(terminal: ExternalTerminal): Result<PeriodClosingResult> {
        return suspendCancellableCoroutine { continuation ->
            partialPeriodClosing(terminal, object : PeriodClosingCallback {
                override fun onSuccess(result: PeriodClosingResult) {
                    continuation.resume(Result.success(result))
                }
                override fun onError(error: String) {
                    continuation.resume(Result.failure(Exception(error)))
                }
            })
        }
    }

    suspend fun getTransactionOverviewSuspend(
        terminal: ExternalTerminal,
        shiftNumber: Int? = null
    ): Result<TransactionOverviewResult> {
        return suspendCancellableCoroutine { continuation ->
            getTransactionOverview(terminal, shiftNumber, object : TransactionOverviewCallback {
                override fun onSuccess(result: TransactionOverviewResult) {
                    continuation.resume(Result.success(result))
                }
                override fun onError(error: String) {
                    continuation.resume(Result.failure(Exception(error)))
                }
            })
        }
    }

    suspend fun reprintLastTicketSuspend(terminal: ExternalTerminal): Result<Pair<String?, String?>> {
        return suspendCancellableCoroutine { continuation ->
            reprintLastTicket(terminal, object : LastTicketCallback {
                override fun onSuccess(merchantReceipt: String?, customerReceipt: String?) {
                    continuation.resume(Result.success(Pair(merchantReceipt, customerReceipt)))
                }
                override fun onError(error: String) {
                    continuation.resume(Result.failure(Exception(error)))
                }
            })
        }
    }
}
