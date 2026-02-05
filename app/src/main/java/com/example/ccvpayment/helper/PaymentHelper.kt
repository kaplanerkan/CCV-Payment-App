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
 * Payment Helper
 *
 * Ödeme işlemleri (satış, iade, iptal, provizyon) için yardımcı sınıf.
 * CCV mAPI SDK ile gerçek entegrasyon.
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
        val payment = Payment.builder()
            .type(Payment.Type.SALE)
            .amount(eu.ccvlab.mapi.core.payment.Money(request.amount.amount, request.amount.currency))
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        val delegate = createPaymentDelegate(request.requestId, callback)
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
        val payment = Payment.builder()
            .type(Payment.Type.REFUND)
            .amount(eu.ccvlab.mapi.core.payment.Money(request.amount.amount, request.amount.currency))
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        val delegate = createPaymentDelegate(request.requestId, callback)
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
        val paymentBuilder = Payment.builder()
            .type(Payment.Type.VOID)
            .posTimestamp(posTimestampFormatter.format(Date()))

        request.amount?.let {
            paymentBuilder.amount(eu.ccvlab.mapi.core.payment.Money(it.amount, it.currency))
        }

        val delegate = createPaymentDelegate(request.originalRequestId, callback)
        paymentService.payment(terminal, paymentBuilder.build(), delegate)
    }

    /**
     * Ön Provizyon (RESERVATION)
     */
    fun authorise(
        terminal: ExternalTerminal,
        request: PaymentRequest,
        callback: PaymentCallback
    ) {
        val payment = Payment.builder()
            .type(Payment.Type.RESERVATION)
            .amount(eu.ccvlab.mapi.core.payment.Money(request.amount.amount, request.amount.currency))
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        val delegate = createPaymentDelegate(request.requestId, callback)
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
        val payment = Payment.builder()
            .type(Payment.Type.SALE)
            .amount(eu.ccvlab.mapi.core.payment.Money(amount.amount, amount.currency))
            .approvalCode(approvalCode)
            .token(token)
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()

        val delegate = createPaymentDelegate(originalRequestId, callback)
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
    private fun createPaymentDelegate(requestId: String, callback: PaymentCallback): PaymentDelegate {
        return object : PaymentDelegate {
            private var merchantReceipt: String? = null
            private var customerReceipt: String? = null

            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {
                lines?.forEach { line ->
                    callback.onDisplayMessage(line.toString())
                }
            }

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                merchantReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {
                customerReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printDccOffer(receipt: PaymentReceipt?) {
                // DCC teklifi göster
            }

            override fun onPaymentSuccess(result: MapiPaymentResult?) {
                val paymentResult = PaymentResult(
                    status = TransactionStatus.SUCCESS,
                    requestId = requestId,
                    amount = result?.amount()?.let { Money(it.value, it.currency) },
                    approvedAmount = result?.amount()?.let { Money(it.value, it.currency) },
                    authCode = result?.approvalCode(),
                    merchantReceipt = merchantReceipt,
                    customerReceipt = customerReceipt
                )

                if (merchantReceipt != null || customerReceipt != null) {
                    callback.onReceiptReady(ReceiptInfo(merchantReceipt, customerReceipt, null))
                }

                callback.onSuccess(paymentResult)
            }

            override fun onError(error: Error?) {
                callback.onError(
                    error?.mapiError()?.name ?: "UNKNOWN",
                    error?.mapiError()?.description() ?: "Bilinmeyen hata"
                )
            }

            override fun drawCustomerSignature(signatureCallback: CustomerSignatureCallback?) {
                callback.onSignatureRequired {
                    signatureCallback?.signature(ByteArray(100))
                }
            }

            override fun askCustomerIdentification(identificationCallback: Callback?) {
                callback.onIdentificationRequired {
                    identificationCallback?.proceed()
                }
            }

            override fun askCustomerSignature(signatureCallback: Callback?) {
                callback.onSignatureRequired {
                    signatureCallback?.proceed()
                }
            }

            override fun askMerchantSignature(signatureCallback: Callback?) {
                signatureCallback?.proceed()
            }

            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {
                // E-fiş işlemleri
            }

            override fun printJournalReceipt(receipt: PaymentReceipt?) {
                // Journal fişi
            }

            override fun storeEJournal(journal: String?) {
                // E-journal kaydet
            }

            override fun showOnCustomerDisplay(mainText: MainTextRequest?, subText: DisplayTextRequest?) {
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
