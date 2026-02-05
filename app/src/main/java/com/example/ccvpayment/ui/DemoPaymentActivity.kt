package com.example.ccvpayment.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ccvpayment.R
import eu.ccvlab.mapi.api.PaymentService
import eu.ccvlab.mapi.api.TerminalService
import eu.ccvlab.mapi.core.Callback
import eu.ccvlab.mapi.core.api.PaymentApi
import eu.ccvlab.mapi.core.api.TerminalApi
import eu.ccvlab.mapi.core.api.response.delegate.PaymentDelegate
import eu.ccvlab.mapi.core.api.response.delegate.TerminalDelegate
import eu.ccvlab.mapi.core.api.response.result.ConfigData
import eu.ccvlab.mapi.core.api.response.result.Error
import eu.ccvlab.mapi.core.machine.CustomerSignatureCallback
import eu.ccvlab.mapi.core.payment.DisplayTextRequest
import eu.ccvlab.mapi.core.payment.EReceiptRequest
import eu.ccvlab.mapi.core.payment.MainTextRequest
import eu.ccvlab.mapi.core.payment.Money
import eu.ccvlab.mapi.core.payment.Payment
import eu.ccvlab.mapi.core.payment.PaymentAdministrationResult
import eu.ccvlab.mapi.core.payment.PaymentReceipt
import eu.ccvlab.mapi.core.payment.PaymentResult
import eu.ccvlab.mapi.core.payment.TextLine
import eu.ccvlab.mapi.core.terminal.ExternalTerminal
import eu.ccvlab.mapi.core.terminal.LanguageCode
import java.math.BigDecimal
import java.util.Currency

/**
 * Demo Payment Activity - 1:1 Kotlin translation of CCV demo's AttendedTerminalActivity.
 *
 * This activity follows the exact same pattern as the official CCV mAPI demo app
 * to test if the payment flow return works correctly.
 */
class DemoPaymentActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DemoPaymentActivity"
    }

    // Flow enum (simplified)
    enum class Flow(val description: String) {
        PAYMENT("Payment"),
        REFUND("Refund"),
        ABORT("Abort")
    }

    // State
    private var activeFlow: Flow? = null
    private var token: String? = null
    private var hashData: String? = null
    private var approvalCode: String? = null

    // Services
    private val paymentService: PaymentApi = PaymentService()
    private val terminalService: TerminalApi = TerminalService()

    // Views
    private lateinit var outputTextField: TextView
    private lateinit var amountInput: EditText
    private lateinit var abortButtonLayout: LinearLayout
    private lateinit var opiFlowLayout: LinearLayout
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_payment)

        // Initialize views
        outputTextField = findViewById(R.id.output)
        amountInput = findViewById(R.id.amount)
        abortButtonLayout = findViewById(R.id.abort_button_layout)
        opiFlowLayout = findViewById(R.id.opi_flow_layout)
        startButton = findViewById(R.id.start_button)

        // Setup scrolling for output
        outputTextField.movementMethod = ScrollingMovementMethod()

        // Setup click listeners
        findViewById<Button>(R.id.start_button).setOnClickListener { onStartClicked(Flow.PAYMENT) }
        findViewById<Button>(R.id.refund_button).setOnClickListener { onStartClicked(Flow.REFUND) }
        findViewById<Button>(R.id.abort_button).setOnClickListener { startFlow(Flow.ABORT) }
        findViewById<Button>(R.id.clear_button).setOnClickListener { outputTextField.text = "" }
    }

    /**
     * Called when Start/Payment button is clicked.
     * Exactly follows demo's onStartClicked() pattern.
     */
    private fun onStartClicked(flow: Flow) {
        // If there is a flow on-going, do nothing
        if (activeFlow == null) {
            outputTextField.text = ""
            startFlow(flow)
            abortButtonLayout.visibility = View.VISIBLE
            opiFlowLayout.visibility = View.GONE
            startAndroid10Intent()
        }
    }

    /**
     * Launch payment screen for Android 10+.
     * Exactly follows demo's startAndroid10Intent() pattern.
     */
    private fun startAndroid10Intent() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val sendIntent = Intent().apply {
                action = "eu.ccv.payment.action.SHOW_PAYMENT"
            }
            if (sendIntent.resolveActivity(packageManager) != null) {
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(sendIntent)
            } else {
                Toast.makeText(this, "couldn't find launcher intent for payment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Start a flow.
     * Exactly follows demo's startFlow() pattern.
     */
    private fun startFlow(flow: Flow) {
        log("Handling flow: $flow")
        outputTextField.text = ""
        activeFlow = flow

        when (flow) {
            Flow.PAYMENT -> {
                val payment = buildPayment()
                val delegate = createPaymentDelegate(Flow.PAYMENT.description)
                paymentService.payment(externalTerminal(), payment, delegate)
            }
            Flow.REFUND -> {
                val payment = Payment.builder()
                    .type(Payment.Type.REFUND)
                    .amount(getAmount())
                    .build()
                val delegate = createPaymentDelegate(Flow.REFUND.description)
                paymentService.payment(externalTerminal(), payment, delegate)
            }
            Flow.ABORT -> {
                val delegate = createTerminalDelegate(Flow.ABORT.description)
                paymentService.abort(externalTerminal(), delegate)
            }
        }
    }

    /**
     * Build payment object.
     */
    private fun buildPayment(): Payment {
        return Payment.builder()
            .type(Payment.Type.SALE)
            .amount(getAmount())
            .build()
    }

    /**
     * Get amount from input field.
     */
    private fun getAmount(): Money {
        val amountText = amountInput.text.toString()
        val amount = if (amountText.isNotEmpty()) BigDecimal(amountText) else BigDecimal("1.00")
        return Money(amount, Currency.getInstance("EUR"))
    }

    /**
     * Create external terminal.
     * Uses OPI-DE protocol with SINGLE_SOCKET mode.
     */
    private fun externalTerminal(): ExternalTerminal {
        return ExternalTerminal.builder()
            .ipAddress("127.0.0.1")
            .port(30002)  // OPI-DE default port is 30002, not 20002
            .socketMode(ExternalTerminal.SocketMode.SINGLE_SOCKET)
            .terminalType(ExternalTerminal.TerminalType.OPI_DE)
            .workstationId("MAPI WORKSTATION")
            .languageCode(LanguageCode.EN)
            .requestToken(true)
            .build()
    }

    /**
     * Create PaymentDelegate.
     * Exactly follows demo's createPaymentDelegate() pattern.
     */
    private fun createPaymentDelegate(context: String): PaymentDelegate {
        return object : PaymentDelegate {
            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {
                lines?.forEach { line ->
                    log("[ $context ]: Terminal output: $line")
                }
            }

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                log("[ $context ]: Print merchant receipt and signature")
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {
                log("[ $context ]: Print customer receipt and signature")
            }

            override fun printDccOffer(receipt: PaymentReceipt?) {
                log("[ $context ]: Print DCC Offer")
            }

            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {
                log("[ $context ]: eReceipt: $eReceiptRequest")
            }

            override fun onPaymentSuccess(paymentResult: PaymentResult?) {
                log("[ $context ]: SUCCESS")
                log("Result: $paymentResult")

                paymentResult?.token()?.takeIf { it.isNotEmpty() }?.let { token = it }
                paymentResult?.hashData()?.takeIf { it.isNotEmpty() }?.let { hashData = it }
                paymentResult?.approvalCode()?.takeIf { it.isNotEmpty() }?.let { approvalCode = it }

                finishFlow()
            }

            override fun onError(error: Error?) {
                log("[ $context ]: ERROR: ${error?.mapiError()?.description()}")
                finishFlow()
            }

            override fun drawCustomerSignature(callback: CustomerSignatureCallback?) {
                log("[ $context ]: Draw customer signature")
                callback?.signature(ByteArray(100))
            }

            override fun askCustomerIdentification(callback: Callback?) {
                log("[ $context ]: Ask customer identification")
                callback?.proceed()
            }

            override fun askCustomerSignature(callback: Callback?) {
                log("[ $context ]: Ask customer signature")
                callback?.proceed()
            }

            override fun askMerchantSignature(callback: Callback?) {
                log("[ $context ]: Ask merchant signature")
                callback?.proceed()
            }

            override fun printJournalReceipt(receipt: PaymentReceipt?) {
                log("[ $context ]: Print journal receipt")
            }

            override fun storeEJournal(journal: String?) {
                log("[ $context ]: Store E-journal: $journal")
            }

            override fun showOnCustomerDisplay(mainText: MainTextRequest?, subText: DisplayTextRequest?) {
                mainText?.text()?.let { log("[ $context ]: Customer Display: $it") }
            }
        }
    }

    /**
     * Create TerminalDelegate.
     * Exactly follows demo's createPaymentAdministrationDelegate() pattern.
     */
    private fun createTerminalDelegate(context: String): TerminalDelegate {
        return object : TerminalDelegate {
            override fun showTerminalOutputLines(lines: MutableList<TextLine>?) {
                lines?.forEach { line ->
                    log("[ $context ]: Terminal output: $line")
                }
            }

            override fun printMerchantReceiptAndSignature(receipt: PaymentReceipt?) {
                log("[ $context ]: Print Merchant Receipt")
            }

            override fun printCustomerReceiptAndSignature(receipt: PaymentReceipt?) {
                log("[ $context ]: Print Customer Receipt")
            }

            override fun printJournalReceipt(receipt: PaymentReceipt?) {
                log("[ $context ]: Journal receipt")
            }

            override fun storeEJournal(journal: String?) {
                log("[ $context ]: Store e-journal: $journal")
            }

            override fun configData(configData: ConfigData?) {
                log("[ $context ]: config data: $configData")
            }

            override fun eReceipt(eReceiptRequest: EReceiptRequest?) {
                log("[ $context ]: eReceipt: $eReceiptRequest")
            }

            override fun cardUID(cardUID: String?) {
                log("[ $context ]: CardUID: $cardUID")
            }

            override fun onPaymentAdministrationSuccess(result: PaymentAdministrationResult<*>?) {
                log("[ $context ]: SUCCESS")
                log("Result: $result")
                finishFlow()
            }

            override fun onError(error: Error?) {
                log("[ $context ]: ERROR: ${error?.mapiError()?.description()}")
                finishFlow()
            }
        }
    }

    /**
     * Finish flow.
     * Exactly follows demo's finishFlow() pattern - just resets UI state.
     */
    private fun finishFlow() {
        activeFlow = null
        runOnUiThread {
            abortButtonLayout.visibility = View.GONE
            opiFlowLayout.visibility = View.VISIBLE
        }
    }

    /**
     * Log to output TextView.
     */
    private fun log(line: String) {
        runOnUiThread {
            outputTextField.append("$line\n")
        }
    }
}
