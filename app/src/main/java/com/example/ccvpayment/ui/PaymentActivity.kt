package com.example.ccvpayment.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.ccvpayment.R
import com.example.ccvpayment.databinding.ActivityPaymentBinding
import com.example.ccvpayment.flow.Flow
import com.example.ccvpayment.flow.FlowActivity
import com.example.ccvpayment.helper.CCVLogger
import com.example.ccvpayment.model.PaymentResult
import com.example.ccvpayment.model.TransactionStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.ccvlab.mapi.core.payment.Payment
import eu.ccvlab.mapi.core.payment.PaymentAdministrationResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Payment Activity - Main screen for payment operations.
 *
 * This activity extends FlowActivity to use the flow-based pattern
 * similar to the CCV demo app. It provides a numpad interface for
 * entering payment amounts and initiating transactions.
 *
 * Features:
 * - Numpad for amount entry
 * - Payment type selection (Sale, Refund, Pre-Auth)
 * - Flow-based state management
 * - Automatic return after payment completes
 *
 * @author Erkan Kaplan
 * @since 1.0
 */
class PaymentActivity : FlowActivity() {

    companion object {
        const val EXTRA_PAYMENT_TYPE = "payment_type"
        const val TYPE_SALE = "sale"
        const val TYPE_REFUND = "refund"
        const val TYPE_AUTHORISE = "authorise"
    }

    private lateinit var binding: ActivityPaymentBinding

    private var currentAmount: Long = 0 // Kuruş cinsinden
    private var paymentType: String = TYPE_SALE

    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.GERMANY))

    // Store last result for showing dialog after flow finishes
    private var lastPaymentResult: PaymentResult? = null
    private var lastErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        paymentType = intent.getStringExtra(EXTRA_PAYMENT_TYPE) ?: TYPE_SALE

        setupToolbar()
        setupPaymentTypeChips()
        setupNumpad()
        setupPayButton()
        updateAmountDisplay()
    }

    // ==================== FLOW ACTIVITY CALLBACKS ====================

    override fun onPaymentSuccess(result: PaymentResult) {
        CCVLogger.logEvent("PAYMENT_COMPLETE", "Payment successful, storing result for dialog")
        lastPaymentResult = result
        lastErrorMessage = null
    }

    override fun onPaymentError(errorCode: String, errorMessage: String) {
        CCVLogger.logEvent("PAYMENT_COMPLETE", "Payment failed: $errorCode - $errorMessage")
        lastPaymentResult = null
        lastErrorMessage = errorMessage
    }

    override fun onTerminalOperationSuccess(result: PaymentAdministrationResult<*>?) {
        // Not used in PaymentActivity
    }

    override fun onTerminalOperationError(errorCode: String, errorMessage: String) {
        // Not used in PaymentActivity
    }

    override fun onFlowFinished() {
        CCVLogger.logEvent("FLOW_FINISHED", "Flow finished, showing result")

        // Hide loading
        hideLoading()

        // Show appropriate dialog based on result
        lastPaymentResult?.let { result ->
            showSuccessDialog(result)
            lastPaymentResult = null
            return
        }

        lastErrorMessage?.let { error ->
            showErrorDialog(error)
            lastErrorMessage = null
            return
        }
    }

    override fun onTerminalOutput(message: String) {
        super.onTerminalOutput(message)
        // Could update a status text view here if needed
    }

    // ==================== UI SETUP ====================

    private fun setupToolbar() {
        val title = when (paymentType) {
            TYPE_SALE -> getString(R.string.menu_payment)
            TYPE_REFUND -> getString(R.string.menu_refund)
            TYPE_AUTHORISE -> getString(R.string.payment_type_authorise)
            else -> getString(R.string.menu_payment)
        }
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupPaymentTypeChips() {
        when (paymentType) {
            TYPE_SALE -> binding.chipSale.isChecked = true
            TYPE_REFUND -> binding.chipRefund.isChecked = true
            TYPE_AUTHORISE -> binding.chipAuthorise.isChecked = true
        }

        binding.chipGroupPaymentType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                paymentType = when (checkedIds.first()) {
                    R.id.chipSale -> TYPE_SALE
                    R.id.chipRefund -> TYPE_REFUND
                    R.id.chipAuthorise -> TYPE_AUTHORISE
                    else -> TYPE_SALE
                }
                updatePayButton()
            }
        }
    }

    private fun setupNumpad() {
        val numpadButtons = listOf(
            binding.btn0 to 0,
            binding.btn1 to 1,
            binding.btn2 to 2,
            binding.btn3 to 3,
            binding.btn4 to 4,
            binding.btn5 to 5,
            binding.btn6 to 6,
            binding.btn7 to 7,
            binding.btn8 to 8,
            binding.btn9 to 9
        )

        numpadButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                appendDigit(digit)
            }
        }

        binding.btnClear.setOnClickListener {
            clearAmount()
        }

        binding.btnDelete.setOnClickListener {
            deleteLastDigit()
        }
    }

    private fun setupPayButton() {
        updatePayButton()

        binding.btnPay.setOnClickListener {
            if (currentAmount > 0 && !isFlowActive()) {
                processPayment()
            }
        }
    }

    // ==================== AMOUNT HANDLING ====================

    private fun appendDigit(digit: Int) {
        // Max 999999.99 (99999999 kuruş)
        if (currentAmount < 99999999) {
            currentAmount = currentAmount * 10 + digit
            updateAmountDisplay()
        }
    }

    private fun deleteLastDigit() {
        currentAmount /= 10
        updateAmountDisplay()
    }

    private fun clearAmount() {
        currentAmount = 0
        updateAmountDisplay()
    }

    private fun updateAmountDisplay() {
        val amount = BigDecimal(currentAmount).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        binding.tvAmount.text = decimalFormat.format(amount)
        updatePayButton()
    }

    private fun updatePayButton() {
        val buttonText = when (paymentType) {
            TYPE_SALE -> getString(R.string.button_pay)
            TYPE_REFUND -> getString(R.string.button_refund)
            TYPE_AUTHORISE -> getString(R.string.payment_type_authorise)
            else -> getString(R.string.button_pay)
        }
        binding.btnPay.text = buttonText
        binding.btnPay.isEnabled = currentAmount > 0 && !isFlowActive()
    }

    // ==================== PAYMENT PROCESSING ====================

    private fun processPayment() {
        val amount = BigDecimal(currentAmount).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

        val loadingMessage = when (paymentType) {
            TYPE_SALE -> getString(R.string.loading_payment)
            TYPE_REFUND -> getString(R.string.loading_refund)
            else -> getString(R.string.loading_please_wait)
        }

        showLoading(loadingMessage)

        // Determine flow and payment type
        val flow: Flow
        val sdkPaymentType: Payment.Type

        when (paymentType) {
            TYPE_REFUND -> {
                flow = Flow.REFUND
                sdkPaymentType = Payment.Type.REFUND
            }
            TYPE_AUTHORISE -> {
                flow = Flow.AUTHORISE
                sdkPaymentType = Payment.Type.RESERVATION
            }
            else -> {
                flow = Flow.PAYMENT
                sdkPaymentType = Payment.Type.SALE
            }
        }

        // Start the flow (sets activeFlow)
        startFlow(flow)

        // Build payment and start via flow handler
        val terminal = getLocalTerminal()
        val payment = buildPayment(sdkPaymentType, amount)

        // Start payment via flow handler (this registers the delegate)
        try {
            when (flow) {
                Flow.REFUND -> flowHandler.startRefund(
                    terminal,
                    sdkPaymentType,
                    eu.ccvlab.mapi.core.payment.Money(amount, java.util.Currency.getInstance("EUR"))
                )
                Flow.AUTHORISE -> flowHandler.startReservation(
                    terminal,
                    sdkPaymentType,
                    eu.ccvlab.mapi.core.payment.Money(amount, java.util.Currency.getInstance("EUR"))
                )
                else -> flowHandler.startPayment(terminal, payment)
            }

            // Launch payment screen AFTER starting the flow (same as demo app)
            launchPaymentScreen()
        } catch (e: UnsupportedOperationException) {
            // SDK doesn't support this operation (e.g., reservation on OPI-DE)
            CCVLogger.logError(flow.name, "UNSUPPORTED", e.message ?: "Operation not supported")
            hideLoading()
            finishFlow()
            showErrorDialog(getString(R.string.error_operation_not_supported))
        }
    }

    // ==================== UI HELPERS ====================

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CCVLogger.logEvent("ON_NEW_INTENT", "PaymentActivity received new intent: ${intent.action}")
        setIntent(intent)

        // Check if this is from flow finish - activity should now be in foreground
        if (intent.getBooleanExtra("from_flow_finish", false)) {
            CCVLogger.logEvent("ON_NEW_INTENT", "Activity brought to foreground from flow finish")
        }
    }

    private fun showSuccessDialog(result: PaymentResult) {
        val message = buildString {
            append(getString(R.string.payment_success_message)).append("\n\n")
            result.transactionId?.let { append(getString(R.string.payment_transaction_id, it)).append("\n") }
            result.amount?.let { append(getString(R.string.payment_amount, formatAmount(it.amount))).append("\n") }
            result.cardBrand?.let { append(getString(R.string.payment_card, it)).append("\n") }
            result.maskedPan?.let { append(getString(R.string.payment_card_number, it)).append("\n") }
            result.authCode?.let { append(getString(R.string.payment_auth_code, it)) }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.result_success))
            .setMessage(message)
            .setIcon(R.drawable.ic_payment)
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ ->
                clearAmount()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(errorMessage: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.result_failed))
            .setMessage(errorMessage)
            .setIcon(R.drawable.ic_refund)
            .setPositiveButton(getString(R.string.dialog_ok), null)
            .setNegativeButton(getString(R.string.button_retry)) { _, _ ->
                processPayment()
            }
            .show()
    }

    private fun showLoading(message: String) {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.tvLoadingMessage.text = message
        updatePayButton() // Disable pay button during flow
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
        updatePayButton() // Re-enable pay button after flow
    }

    private fun formatAmount(amount: BigDecimal): String {
        return "€ ${decimalFormat.format(amount)}"
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isFlowActive()) {
            // Abort the current flow
            flowHandler.startAbort(getLocalTerminal())
        } else {
            super.onBackPressed()
        }
    }
}
