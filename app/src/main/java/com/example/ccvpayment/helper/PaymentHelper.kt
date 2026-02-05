package com.example.ccvpayment.helper

import eu.ccvlab.mapi.api.PaymentService
import eu.ccvlab.mapi.core.Callback
import eu.ccvlab.mapi.core.api.PaymentApi
import eu.ccvlab.mapi.core.api.response.delegate.PaymentDelegate
import eu.ccvlab.mapi.core.api.response.delegate.TerminalDelegate
import eu.ccvlab.mapi.core.api.response.result.ConfigData
import eu.ccvlab.mapi.core.api.response.result.Error
import eu.ccvlab.mapi.core.machine.CustomerSignatureCallback
import eu.ccvlab.mapi.core.payment.Payment
import eu.ccvlab.mapi.core.payment.PaymentAdministrationResult
import eu.ccvlab.mapi.core.payment.PaymentReceipt
import eu.ccvlab.mapi.core.payment.PaymentResult as MapiPaymentResult
import eu.ccvlab.mapi.core.payment.TextLine
import eu.ccvlab.mapi.core.payment.EReceiptRequest
import eu.ccvlab.mapi.core.payment.DisplayTextRequest
import eu.ccvlab.mapi.core.payment.MainTextRequest
import eu.ccvlab.mapi.core.terminal.ExternalTerminal
import com.example.ccvpayment.model.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Payment Helper - Handles payment transaction operations.
 *
 * This singleton class provides methods for executing various payment
 * operations through the CCV mAPI SDK, including sales, refunds,
 * reversals, and pre-authorizations.
 *
 * Features:
 * - Sale transactions (SALE)
 * - Refund transactions (REFUND)
 * - Reversal/void transactions (VOID)
 * - Pre-authorization (RESERVATION)
 * - Capture after pre-authorization
 * - Payment abort
 *
 * Both callback-based and coroutine-based APIs are available.
 *
 * @author Erkan Kaplan
 * @date 2026-02-05
 * @since 1.0
 */
class PaymentHelper private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: PaymentHelper? = null

        fun getInstance(): PaymentHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PaymentHelper().also { INSTANCE = it }
            }
        }
    }

    private val paymentService: PaymentApi = PaymentService()
    private val posTimestampFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.getDefault())

    /**
     * Callback Arayüzleri
     */
    interface PaymentCallback {
        fun onSuccess(result: PaymentResult)
        fun onError(errorCode: String, errorMessage: String)
        fun onReceiptReady(receipt: ReceiptInfo) {}
        fun onSignatureRequired(onSignatureConfirmed: () -> Unit) {
            onSignatureConfirmed()
        }
        fun onIdentificationRequired(onIdentificationConfirmed: () -> Unit) {
            onIdentificationConfirmed()
        }
        fun onDisplayMessage(message: String) {}
    }

    /**
     * Satış İşlemi (SALE)
     */
    fun makePayment(
        terminal: ExternalTerminal,
        request: PaymentRequest,
        callback: PaymentCallback,
        receiptMode: ReceiptMode = ReceiptMode.RECEIPTS_IN_RESPONSE
    ) {
        // Log outgoing request
        CCVLogger.logPaymentRequest("SALE", terminal, request)

        val payment = Payment.builder()
            .type(Payment.Type.SALE)
            .amount(eu.ccvlab.mapi.core.payment.Money(request.amount.amount, request.amount.currency))
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        // Log raw SDK payment object
        CCVLogger.logRawPayment(payment)

        val delegate = createPaymentDelegate(request.requestId, "SALE", callback)
        paymentService.payment(terminal, payment, delegate)
    }

    /**
     * İade İşlemi (REFUND)
     */
    fun refund(
        terminal: ExternalTerminal,
        request: RefundRequest,
        callback: PaymentCallback
    ) {
        // Log outgoing request
        CCVLogger.logRefundRequest(terminal, request)

        val payment = Payment.builder()
            .type(Payment.Type.REFUND)
            .amount(eu.ccvlab.mapi.core.payment.Money(request.amount.amount, request.amount.currency))
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        // Log raw SDK payment object
        CCVLogger.logRawPayment(payment)

        val delegate = createPaymentDelegate(request.requestId, "REFUND", callback)
        paymentService.payment(terminal, payment, delegate)
    }

    /**
     * İptal İşlemi (VOID)
     */
    fun reversal(
        terminal: ExternalTerminal,
        request: ReversalRequest,
        callback: PaymentCallback
    ) {
        // Log outgoing request
        CCVLogger.logReversalRequest(terminal, request)

        val paymentBuilder = Payment.builder()
            .type(Payment.Type.VOID)
            .posTimestamp(posTimestampFormatter.format(Date()))

        request.amount?.let {
            paymentBuilder.amount(eu.ccvlab.mapi.core.payment.Money(it.amount, it.currency))
        }

        val payment = paymentBuilder.build()

        // Log raw SDK payment object
        CCVLogger.logRawPayment(payment)

        val delegate = createPaymentDelegate(request.originalRequestId, "REVERSAL", callback)
        paymentService.payment(terminal, payment, delegate)
    }

    /**
     * Ön Provizyon (RESERVATION)
     */
    fun authorise(
        terminal: ExternalTerminal,
        request: PaymentRequest,
        callback: PaymentCallback
    ) {
        // Log outgoing request
        CCVLogger.logPaymentRequest("AUTHORISE", terminal, request)

        val payment = Payment.builder()
            .type(Payment.Type.RESERVATION)
            .amount(eu.ccvlab.mapi.core.payment.Money(request.amount.amount, request.amount.currency))
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        // Log raw SDK payment object
        CCVLogger.logRawPayment(payment)

        val delegate = createPaymentDelegate(request.requestId, "AUTHORISE", callback)
        paymentService.reservation(terminal, payment, delegate)
    }

    /**
     * Provizyon Kapama (SALE after RESERVATION)
     */
    fun capture(
        terminal: ExternalTerminal,
        originalRequestId: String,
        amount: Money,
        callback: PaymentCallback,
        approvalCode: String? = null,
        token: String? = null
    ) {
        // Log outgoing request
        CCVLogger.logTerminalRequest("CAPTURE", terminal, mapOf(
            "originalRequestId" to originalRequestId,
            "amount" to amount.formatted(),
            "approvalCode" to approvalCode,
            "token" to token
        ))

        val payment = Payment.builder()
            .type(Payment.Type.SALE)
            .amount(eu.ccvlab.mapi.core.payment.Money(amount.amount, amount.currency))
            .approvalCode(approvalCode)
            .token(token)
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        // Log raw SDK payment object
        CCVLogger.logRawPayment(payment)

        val delegate = createPaymentDelegate(originalRequestId, "CAPTURE", callback)
        paymentService.payment(terminal, payment, delegate)
    }

    /**
     * Ödeme Kurtarma (Recovery)
     */
    fun recoverPayment(
        terminal: ExternalTerminal,
        originalRequestId: String,
        callback: PaymentCallback
    ) {
        // TerminalService üzerinden recover yapılır
        callback.onError("NOT_IMPLEMENTED", "Use TerminalHelper.recoverPayment instead")
    }

    /**
     * Devam Eden Ödemeyi Durdur (Abort)
     */
    fun stopPayment(terminal: ExternalTerminal, callback: PaymentCallback) {
        CCVLogger.logTerminalRequest("ABORT", terminal)
        val delegate = createTerminalDelegate(callback)
        paymentService.abort(terminal, delegate)
    }

    /**
     * TerminalDelegate oluştur (abort için)
     */
    private fun createTerminalDelegate(callback: PaymentCallback): TerminalDelegate {
        return object : TerminalDelegate {
            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {
                lines?.forEach { line ->
                    callback.onDisplayMessage(line.toString())
                }
            }

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {}
            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {}
            override fun printJournalReceipt(receipt: PaymentReceipt?) {}
            override fun storeEJournal(journal: String?) {}
            override fun configData(config: ConfigData?) {}
            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {}
            override fun cardUID(cardUID: String?) {}

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                callback.onSuccess(PaymentResult(
                    status = TransactionStatus.SUCCESS,
                    errorMessage = "Operation completed"
                ))
            }

            override fun onError(error: Error?) {
                callback.onError(
                    error?.mapiError()?.name ?: "UNKNOWN",
                    error?.mapiError()?.description() ?: "Bilinmeyen hata"
                )
            }
        }
    }

    /**
     * PaymentDelegate oluştur
     */
    private fun createPaymentDelegate(requestId: String, operation: String, callback: PaymentCallback): PaymentDelegate {
        return object : PaymentDelegate {
            private var merchantReceipt: String? = null
            private var customerReceipt: String? = null

            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {
                lines?.forEach { line ->
                    CCVLogger.logEvent("TERMINAL_OUTPUT", line.toString())
                    callback.onDisplayMessage(line.toString())
                }
            }

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                merchantReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
                CCVLogger.logEvent("MERCHANT_RECEIPT", "Receipt received")
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {
                customerReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
                CCVLogger.logEvent("CUSTOMER_RECEIPT", "Receipt received")
            }

            override fun printDccOffer(receipt: PaymentReceipt?) {
                CCVLogger.logEvent("DCC_OFFER", "DCC offer received")
            }

            override fun onPaymentSuccess(result: MapiPaymentResult?) {
                // Log raw SDK result
                CCVLogger.logRawPaymentResult(result)

                val paymentResult = PaymentResult(
                    status = TransactionStatus.SUCCESS,
                    requestId = requestId,
                    amount = result?.amount()?.let { Money(it.value, it.currency) },
                    approvedAmount = result?.amount()?.let { Money(it.value, it.currency) },
                    authCode = result?.approvalCode(),
                    merchantReceipt = merchantReceipt,
                    customerReceipt = customerReceipt
                )

                // Log response
                CCVLogger.logPaymentResponse(operation, paymentResult)

                if (merchantReceipt != null || customerReceipt != null) {
                    callback.onReceiptReady(ReceiptInfo(merchantReceipt, customerReceipt, null))
                }

                callback.onSuccess(paymentResult)
            }

            override fun onError(error: Error?) {
                val errorCode = error?.mapiError()?.name ?: "UNKNOWN"
                val errorMessage = error?.mapiError()?.description() ?: "Unknown error"

                // Log error
                CCVLogger.logError(operation, errorCode, errorMessage)

                callback.onError(errorCode, errorMessage)
            }

            override fun drawCustomerSignature(signatureCallback: CustomerSignatureCallback?) {
                CCVLogger.logEvent("SIGNATURE_REQUEST", "Customer signature requested")
                callback.onSignatureRequired {
                    signatureCallback?.signature(ByteArray(100))
                }
            }

            override fun askCustomerIdentification(identificationCallback: Callback?) {
                CCVLogger.logEvent("IDENTIFICATION_REQUEST", "Customer identification requested")
                callback.onIdentificationRequired {
                    identificationCallback?.proceed()
                }
            }

            override fun askCustomerSignature(signatureCallback: Callback?) {
                CCVLogger.logEvent("SIGNATURE_REQUEST", "Customer signature confirmation requested")
                callback.onSignatureRequired {
                    signatureCallback?.proceed()
                }
            }

            override fun askMerchantSignature(signatureCallback: Callback?) {
                CCVLogger.logEvent("SIGNATURE_REQUEST", "Merchant signature requested")
                signatureCallback?.proceed()
            }

            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {
                CCVLogger.logEvent("E_RECEIPT", eReceiptRequest?.toString())
            }

            override fun printJournalReceipt(receipt: PaymentReceipt?) {
                CCVLogger.logEvent("JOURNAL_RECEIPT", "Journal receipt received")
            }

            override fun storeEJournal(journal: String?) {
                CCVLogger.logEvent("E_JOURNAL", "E-Journal stored")
            }

            override fun showOnCustomerDisplay(mainText: MainTextRequest?, subText: DisplayTextRequest?) {
                CCVLogger.logEvent("CUSTOMER_DISPLAY", mainText?.text())
                mainText?.text()?.let { callback.onDisplayMessage(it) }
            }
        }
    }

    /**
     * Coroutine versiyonları
     */
    suspend fun makePaymentSuspend(
        terminal: ExternalTerminal,
        request: PaymentRequest
    ): Result<PaymentResult> {
        return suspendCancellableCoroutine { continuation ->
            makePayment(terminal, request, object : PaymentCallback {
                override fun onSuccess(result: PaymentResult) {
                    continuation.resume(Result.success(result))
                }
                override fun onError(errorCode: String, errorMessage: String) {
                    continuation.resume(Result.failure(PaymentException(errorCode, errorMessage)))
                }
            })
        }
    }

    suspend fun refundSuspend(
        terminal: ExternalTerminal,
        request: RefundRequest
    ): Result<PaymentResult> {
        return suspendCancellableCoroutine { continuation ->
            refund(terminal, request, object : PaymentCallback {
                override fun onSuccess(result: PaymentResult) {
                    continuation.resume(Result.success(result))
                }
                override fun onError(errorCode: String, errorMessage: String) {
                    continuation.resume(Result.failure(PaymentException(errorCode, errorMessage)))
                }
            })
        }
    }
}

/**
 * Ödeme Hatası
 */
class PaymentException(
    val errorCode: String,
    override val message: String
) : Exception(message)
