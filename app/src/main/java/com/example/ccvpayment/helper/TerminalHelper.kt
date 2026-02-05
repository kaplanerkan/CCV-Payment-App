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
import eu.ccvlab.mapi.core.terminal.LanguageCode
import com.example.ccvpayment.model.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Terminal Helper
 *
 * Terminal bağlantısı, keşfi ve durum sorgulama işlemleri için yardımcı sınıf.
 * CCV mAPI SDK ile gerçek entegrasyon.
 */
class TerminalHelper private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: TerminalHelper? = null

        fun getInstance(): TerminalHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TerminalHelper().also { INSTANCE = it }
            }
        }

        // Default port değerleri
        const val DEFAULT_OPI_NL_PORT = 4100
        const val DEFAULT_OPI_NL_COMPAT_PORT = 4102
        const val DEFAULT_OPI_DE_PORT = 20002
        const val DEFAULT_OPI_DE_COMPAT_PORT = 20007
        const val LOCAL_TERMINAL_IP = "127.0.0.1"
    }

    private val terminalService: TerminalApi = TerminalService()

    // Terminal bilgilerini saklamak için map
    private val terminalInfoMap = mutableMapOf<ExternalTerminal, TerminalInfo>()

    /**
     * Callback Arayüzleri
     */
    interface TerminalDiscoveryCallback {
        fun onTerminalFound(terminal: ExternalTerminal)
        fun onDiscoveryComplete(terminals: List<ExternalTerminal>)
        fun onError(error: String)
    }

    interface TerminalStatusCallback {
        fun onStatusReceived(status: TerminalStatus)
        fun onError(error: String)
    }

    interface TerminalOperationCallback {
        fun onSuccess(message: String? = null)
        fun onError(error: String)
    }

    /**
     * Yerel terminal bilgisini al (Android POS cihazları için)
     */
    fun getLocalTerminal(protocol: TerminalProtocol = TerminalProtocol.OPI_DE): ExternalTerminal {
        val port = when (protocol) {
            TerminalProtocol.OPI_DE -> DEFAULT_OPI_DE_PORT
            TerminalProtocol.OPI_NL -> DEFAULT_OPI_NL_PORT
        }
        return createTerminalInternal(LOCAL_TERMINAL_IP, port, protocol)
    }

    /**
     * Özel terminal oluştur
     */
    fun createTerminal(
        ipAddress: String,
        port: Int? = null,
        protocol: TerminalProtocol = TerminalProtocol.OPI_DE
    ): ExternalTerminal {
        val defaultPort = when (protocol) {
            TerminalProtocol.OPI_DE -> DEFAULT_OPI_DE_PORT
            TerminalProtocol.OPI_NL -> DEFAULT_OPI_NL_PORT
        }
        return createTerminalInternal(ipAddress, port ?: defaultPort, protocol)
    }

    private fun createTerminalInternal(ipAddress: String, port: Int, protocol: TerminalProtocol): ExternalTerminal {
        val compatPort = when (protocol) {
            TerminalProtocol.OPI_DE -> DEFAULT_OPI_DE_COMPAT_PORT
            TerminalProtocol.OPI_NL -> DEFAULT_OPI_NL_COMPAT_PORT
        }

        val terminalType = when (protocol) {
            TerminalProtocol.OPI_DE -> ExternalTerminal.TerminalType.OPI_DE
            TerminalProtocol.OPI_NL -> ExternalTerminal.TerminalType.OPI_NL
        }

        val socketMode = when (protocol) {
            TerminalProtocol.OPI_DE -> ExternalTerminal.SocketMode.SINGLE_SOCKET
            TerminalProtocol.OPI_NL -> ExternalTerminal.SocketMode.DUAL_SOCKET
        }

        // SDK'nın ExternalTerminal.builder() API'sini kullan
        val terminal = ExternalTerminal.builder()
            .ipAddress(ipAddress)
            .port(port)
            .compatibilityPort(compatPort)
            .socketMode(socketMode)
            .terminalType(terminalType)
            .workstationId("CCV_PAYMENT_APP")
            .languageCode(LanguageCode.EN)
            .requestToken(true)
            .build()

        // Terminal bilgisini sakla
        terminalInfoMap[terminal] = TerminalInfo(
            ipAddress = ipAddress,
            port = port,
            compatibilityPort = compatPort,
            protocol = protocol,
            socketMode = when (protocol) {
                TerminalProtocol.OPI_DE -> SocketMode.SINGLE_SOCKET
                TerminalProtocol.OPI_NL -> SocketMode.DUAL_SOCKET
            }
        )

        return terminal
    }

    /**
     * Terminal bilgisini al
     */
    fun getTerminalInfo(terminal: ExternalTerminal): TerminalInfo? {
        return terminalInfoMap[terminal]
    }

    /**
     * Ağda terminal keşfet
     */
    fun discoverTerminals(callback: TerminalDiscoveryCallback) {
        // Yerel terminal'i döndür
        val localTerminal = getLocalTerminal()
        callback.onTerminalFound(localTerminal)
        callback.onDiscoveryComplete(listOf(localTerminal))
    }

    /**
     * Coroutine versiyonu - Terminal keşfi
     */
    suspend fun discoverTerminalsSuspend(): Result<List<ExternalTerminal>> {
        return suspendCancellableCoroutine { continuation ->
            discoverTerminals(object : TerminalDiscoveryCallback {
                override fun onTerminalFound(terminal: ExternalTerminal) {}

                override fun onDiscoveryComplete(terminals: List<ExternalTerminal>) {
                    continuation.resume(Result.success(terminals))
                }

                override fun onError(error: String) {
                    continuation.resume(Result.failure(Exception(error)))
                }
            })
        }
    }

    /**
     * Terminal durumunu sorgula
     */
    fun getStatus(terminal: ExternalTerminal, callback: TerminalStatusCallback) {
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
                val info = terminalInfoMap[terminal]
                callback.onStatusReceived(TerminalStatus(
                    isConnected = true,
                    terminalId = result?.terminalId(),
                    softwareVersion = null,
                    ipAddress = info?.ipAddress ?: LOCAL_TERMINAL_IP,
                    printerStatus = PrinterStatus.AVAILABLE
                ))
            }

            override fun onError(error: Error?) {
                callback.onError(error?.mapiError()?.description() ?: "Status query failed")
            }
        }

        terminalService.status(terminal, delegate)
    }

    /**
     * Coroutine versiyonu - Terminal durumu
     */
    suspend fun getStatusSuspend(terminal: ExternalTerminal): Result<TerminalStatus> {
        return suspendCancellableCoroutine { continuation ->
            getStatus(terminal, object : TerminalStatusCallback {
                override fun onStatusReceived(status: TerminalStatus) {
                    continuation.resume(Result.success(status))
                }

                override fun onError(error: String) {
                    continuation.resume(Result.failure(Exception(error)))
                }
            })
        }
    }

    /**
     * Terminal başlat (Startup)
     */
    fun startup(terminal: ExternalTerminal, callback: TerminalOperationCallback) {
        val delegate = createTerminalDelegate(callback)
        terminalService.startup(terminal, delegate)
    }

    /**
     * Terminal aktivasyonu
     */
    fun activateTerminal(
        terminal: ExternalTerminal,
        callback: TerminalOperationCallback
    ) {
        val delegate = createTerminalDelegate(callback)
        terminalService.activateTerminal(terminal, delegate)
    }

    /**
     * Factory Reset - Not available in this SDK version
     */
    fun factoryReset(terminal: ExternalTerminal, callback: TerminalOperationCallback) {
        callback.onError("Factory reset not available in this SDK version")
    }

    /**
     * Son mesajı tekrarla
     */
    fun repeatLastMessage(terminal: ExternalTerminal, callback: TerminalOperationCallback) {
        val delegate = createTerminalDelegate(callback)
        terminalService.repeatLastMessage(terminal, delegate)
    }

    /**
     * Son fişi al
     */
    fun retrieveLastTicket(terminal: ExternalTerminal, callback: TerminalOperationCallback) {
        val delegate = createTerminalDelegate(callback)
        terminalService.retrieveLastTicket(terminal, delegate)
    }

    /**
     * Payment Recovery
     */
    fun recoverPayment(terminal: ExternalTerminal, paymentRequestId: String, callback: TerminalOperationCallback) {
        val delegate = createTerminalDelegate(callback)
        terminalService.recoverPayment(terminal, paymentRequestId, delegate)
    }

    /**
     * Period Closing (Z-Report)
     */
    fun periodClosing(terminal: ExternalTerminal, callback: TerminalOperationCallback) {
        val delegate = createTerminalDelegate(callback)
        terminalService.periodClosing(terminal, delegate)
    }

    /**
     * Transaction Overview
     */
    fun transactionOverview(terminal: ExternalTerminal, callback: TerminalOperationCallback) {
        val delegate = createTerminalDelegate(callback)
        terminalService.transactionOverview(terminal, delegate)
    }

    private fun createTerminalDelegate(callback: TerminalOperationCallback): TerminalDelegate {
        return object : TerminalDelegate {
            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {}
            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {}
            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {}
            override fun printJournalReceipt(receipt: PaymentReceipt?) {}
            override fun storeEJournal(journal: String?) {}
            override fun configData(config: ConfigData?) {}
            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {}
            override fun cardUID(cardUID: String?) {}

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                callback.onSuccess(result?.toString())
            }

            override fun onError(error: Error?) {
                callback.onError(error?.mapiError()?.description() ?: "Operation failed")
            }
        }
    }
}
