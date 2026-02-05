package com.example.ccvpayment.flow

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ccvpayment.helper.CCVLogger
import com.example.ccvpayment.helper.TerminalHelper
import com.example.ccvpayment.model.Money
import com.example.ccvpayment.model.PaymentResult
import com.example.ccvpayment.model.TransactionStatus
import eu.ccvlab.mapi.core.api.response.delegate.PaymentDelegate
import eu.ccvlab.mapi.core.api.response.delegate.TerminalDelegate
import eu.ccvlab.mapi.core.api.response.result.ConfigData
import eu.ccvlab.mapi.core.api.response.result.Error
import eu.ccvlab.mapi.core.machine.CustomerSignatureCallback
import eu.ccvlab.mapi.core.payment.DisplayTextRequest
import eu.ccvlab.mapi.core.payment.EReceiptRequest
import eu.ccvlab.mapi.core.payment.MainTextRequest
import eu.ccvlab.mapi.core.payment.Payment
import eu.ccvlab.mapi.core.payment.PaymentAdministrationResult
import eu.ccvlab.mapi.core.payment.PaymentReceipt
import eu.ccvlab.mapi.core.payment.TextLine
import eu.ccvlab.mapi.core.terminal.ExternalTerminal
import eu.ccvlab.mapi.core.payment.PaymentResult as MapiPaymentResult
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FlowActivity - Base activity for flow-based payment operations.
 *
 * This abstract class provides the flow management infrastructure
 * similar to the CCV demo app. Activities that need to perform
 * payment operations should extend this class.
 *
 * Key features:
 * - activeFlow state management
 * - DelegationFactory implementation
 * - startFlow() / finishFlow() pattern
 * - Automatic SHOW_PAYMENT intent for Android 10+
 *
 * @author Erkan Kaplan
 * @since 1.0
 */
abstract class FlowActivity : AppCompatActivity(), DelegationFactory {

    // Flow state - null means no operation in progress
    protected var activeFlow: Flow? = null
        private set

    // Flow handler for OPI-DE protocol
    protected val flowHandler: PaymentFlowHandler by lazy {
        OpiDeFlowHandler(this)
    }

    // Terminal helper for creating terminals
    protected val terminalHelper: TerminalHelper by lazy {
        TerminalHelper.getInstance()
    }

    // Stored values from payment results
    protected var token: String? = null
    protected var hashData: String? = null
    protected var approvalCode: String? = null
    protected var lastTransactionId: String? = null

    // Receipt storage
    protected var merchantReceipt: String? = null
    protected var customerReceipt: String? = null

    private val posTimestampFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.getDefault())

    // ==================== ABSTRACT METHODS ====================

    /**
     * Called when a payment operation succeeds.
     * Subclasses should update UI accordingly.
     */
    abstract fun onPaymentSuccess(result: PaymentResult)

    /**
     * Called when a payment operation fails.
     * Subclasses should show error message.
     */
    abstract fun onPaymentError(errorCode: String, errorMessage: String)

    /**
     * Called when a terminal operation succeeds.
     * Subclasses should update UI accordingly.
     */
    abstract fun onTerminalOperationSuccess(result: PaymentAdministrationResult<*>?)

    /**
     * Called when a terminal operation fails.
     * Subclasses should show error message.
     */
    abstract fun onTerminalOperationError(errorCode: String, errorMessage: String)

    /**
     * Called when flow finishes (success or error).
     * Subclasses should reset UI state.
     */
    abstract fun onFlowFinished()

    /**
     * Called when terminal output is received.
     * Subclasses can display status messages.
     */
    open fun onTerminalOutput(message: String) {
        CCVLogger.logEvent("TERMINAL_OUTPUT", message)
    }

    // ==================== FLOW MANAGEMENT ====================

    /**
     * Check if a flow is currently active.
     */
    fun isFlowActive(): Boolean = activeFlow != null

    /**
     * Start a new flow.
     * Does nothing if a flow is already active.
     */
    protected fun startFlow(flow: Flow) {
        if (activeFlow != null) {
            CCVLogger.logEvent("FLOW_BLOCKED", "Cannot start $flow, active flow: $activeFlow")
            return
        }

        CCVLogger.logEvent("FLOW_START", "Starting flow: $flow")
        activeFlow = flow

        // Clear receipt storage
        merchantReceipt = null
        customerReceipt = null
    }

    /**
     * Finish the current flow.
     * Resets activeFlow to null and notifies subclass.
     * Following demo pattern - no bringToForeground() needed.
     */
    protected fun finishFlow() {
        CCVLogger.logEvent("FLOW_FINISH", "Finishing flow: $activeFlow")
        activeFlow = null

        // Call onFlowFinished on UI thread (like demo's finishFlow)
        runOnUiThread {
            onFlowFinished()
        }
    }

    /**
     * Launch the CCV payment screen on Android 10+.
     */
    protected fun launchPaymentScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent().apply {
                action = "eu.ccv.payment.action.SHOW_PAYMENT"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                CCVLogger.logEvent("SHOW_PAYMENT", "Launched payment screen intent")
            }
        }
    }

    /**
     * Get the local terminal (127.0.0.1:30002).
     */
    protected fun getLocalTerminal(): ExternalTerminal {
        return terminalHelper.getLocalTerminal()
    }

    /**
     * Build a payment object for the given amount.
     */
    protected fun buildPayment(
        type: Payment.Type,
        amount: BigDecimal,
        currency: java.util.Currency = java.util.Currency.getInstance("EUR")
    ): Payment {
        return Payment.builder()
            .type(type)
            .amount(eu.ccvlab.mapi.core.payment.Money(amount, currency))
            .posTimestamp(posTimestampFormatter.format(Date()))
            .build()
    }

    // ==================== DELEGATION FACTORY ====================

    override fun createPaymentDelegate(context: String): PaymentDelegate {
        return object : PaymentDelegate {
            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {
                lines?.forEach { line ->
                    onTerminalOutput(line.text() ?: "")
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

            override fun onPaymentSuccess(paymentResult: MapiPaymentResult?) {
                CCVLogger.logEvent("PAYMENT_SUCCESS", "[$context] Payment successful")

                // Store values for future operations
                paymentResult?.token()?.takeIf { it.isNotEmpty() }?.let { token = it }
                paymentResult?.hashData()?.takeIf { it.isNotEmpty() }?.let { hashData = it }
                paymentResult?.approvalCode()?.takeIf { it.isNotEmpty() }?.let { approvalCode = it }

                val result = PaymentResult(
                    status = TransactionStatus.SUCCESS,
                    amount = paymentResult?.amount()?.let { Money(it.value, it.currency) },
                    approvedAmount = paymentResult?.amount()?.let { Money(it.value, it.currency) },
                    authCode = paymentResult?.approvalCode(),
                    merchantReceipt = merchantReceipt,
                    customerReceipt = customerReceipt
                )

                runOnUiThread {
                    this@FlowActivity.onPaymentSuccess(result)
                }
                finishFlow()
            }

            override fun onError(error: Error?) {
                val errorCode = error?.mapiError()?.name ?: "UNKNOWN"
                val errorMessage = error?.mapiError()?.description() ?: "Unknown error"
                CCVLogger.logError(context, errorCode, errorMessage)

                runOnUiThread {
                    this@FlowActivity.onPaymentError(errorCode, errorMessage)
                }
                finishFlow()
            }

            override fun drawCustomerSignature(signatureCallback: CustomerSignatureCallback?) {
                CCVLogger.logEvent("SIGNATURE_REQUEST", "Customer signature requested")
                signatureCallback?.signature(ByteArray(100))
            }

            override fun askCustomerIdentification(callback: eu.ccvlab.mapi.core.Callback?) {
                CCVLogger.logEvent("IDENTIFICATION_REQUEST", "Customer identification requested")
                callback?.proceed()
            }

            override fun askCustomerSignature(callback: eu.ccvlab.mapi.core.Callback?) {
                CCVLogger.logEvent("SIGNATURE_CONFIRM", "Customer signature confirmation requested")
                callback?.proceed()
            }

            override fun askMerchantSignature(callback: eu.ccvlab.mapi.core.Callback?) {
                CCVLogger.logEvent("MERCHANT_SIGNATURE", "Merchant signature requested")
                callback?.proceed()
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
                mainText?.text()?.let { onTerminalOutput(it) }
            }
        }
    }

    override fun createTerminalDelegate(context: String): TerminalDelegate {
        return object : TerminalDelegate {
            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {
                lines?.forEach { line ->
                    onTerminalOutput(line.text() ?: "")
                }
            }

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                merchantReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {
                customerReceipt = receipt?.plainTextLines()?.joinToString("\n") { it.toString() }
            }

            override fun printJournalReceipt(receipt: PaymentReceipt?) {
                CCVLogger.logEvent("JOURNAL_RECEIPT", "Journal receipt received")
            }

            override fun storeEJournal(journal: String?) {
                CCVLogger.logEvent("E_JOURNAL", "E-Journal stored")
            }

            override fun configData(config: ConfigData?) {
                CCVLogger.logEvent("CONFIG_DATA", config?.toString())
            }

            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {
                CCVLogger.logEvent("E_RECEIPT", eReceiptRequest?.toString())
            }

            override fun cardUID(cardUID: String?) {
                CCVLogger.logEvent("CARD_UID", cardUID)
            }

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                CCVLogger.logEvent("TERMINAL_SUCCESS", "[$context] Operation successful")
                runOnUiThread {
                    this@FlowActivity.onTerminalOperationSuccess(result)
                }
                finishFlow()
            }

            override fun onError(error: Error?) {
                val errorCode = error?.mapiError()?.name ?: "UNKNOWN"
                val errorMessage = error?.mapiError()?.description() ?: "Unknown error"
                CCVLogger.logError(context, errorCode, errorMessage)

                runOnUiThread {
                    this@FlowActivity.onTerminalOperationError(errorCode, errorMessage)
                }
                finishFlow()
            }
        }
    }
}
