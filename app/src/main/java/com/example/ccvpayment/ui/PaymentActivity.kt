package com.example.ccvpayment.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ccvpayment.R
import com.example.ccvpayment.databinding.ActivityPaymentBinding
import com.example.ccvpayment.helper.CCVLogger
import com.example.ccvpayment.helper.CCVPaymentManager
import com.example.ccvpayment.model.TransactionStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Payment Activity - Main screen for payment operations.
 *
 * This activity provides a numpad interface for entering payment amounts
 * and initiating sale, refund, and pre-authorization transactions.
 *
 * Features:
 * - Numpad for amount entry
 * - Payment type selection (Sale, Refund, Pre-Auth)
 * - Loading overlay during payment processing
 * - Success/error dialogs with transaction details
 * - Automatic return to foreground after payment completes
 *
 * @author Erkan Kaplan
 * @date 2026-02-05
 * @since 1.0
 */
class PaymentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAYMENT_TYPE = "payment_type"
        const val TYPE_SALE = "sale"
        const val TYPE_REFUND = "refund"
        const val TYPE_AUTHORISE = "authorise"
    }

    private lateinit var binding: ActivityPaymentBinding
    private val ccv = CCVPaymentManager.getInstance()

    private var currentAmount: Long = 0 // Kuruş cinsinden
    private var paymentType: String = TYPE_SALE

    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.GERMANY))

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
            if (currentAmount > 0) {
                processPayment()
            }
        }
    }

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
        binding.btnPay.isEnabled = currentAmount > 0
    }

    private fun processPayment() {
        val amount = BigDecimal(currentAmount).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

        val loadingMessage = when (paymentType) {
            TYPE_SALE -> getString(R.string.loading_payment)
            TYPE_REFUND -> getString(R.string.loading_refund)
            else -> getString(R.string.loading_please_wait)
        }

        showLoading(loadingMessage)

        // Launch payment screen on Android 10+ devices
        launchPaymentScreen()

        lifecycleScope.launch {
            try {
                val result = when (paymentType) {
                    TYPE_REFUND -> ccv.refundAsync(amount)
                    else -> ccv.makePaymentAsync(amount)
                }

                // Bring this activity back to foreground after payment completes
                bringToForeground()

                hideLoading()

                if (result.status == TransactionStatus.SUCCESS) {
                    showSuccessDialog(result)
                } else {
                    showErrorDialog(result.errorMessage ?: getString(R.string.error_transaction_failed))
                }
            } catch (e: Exception) {
                // Bring this activity back to foreground on error
                bringToForeground()

                hideLoading()
                showErrorDialog(e.message ?: getString(R.string.error_unknown))
            }
        }
    }

    /**
     * Launch the CCV payment screen on Android 10+ devices.
     *
     * On Android 10 and later, apps cannot start activities from the background.
     * The CCV terminal app provides an intent action to show the payment UI.
     */
    private fun launchPaymentScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent().apply {
                action = "eu.ccv.payment.action.SHOW_PAYMENT"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    /**
     * Bring this activity back to the foreground after payment completes.
     *
     * This ensures the user returns to our app after the payment flow
     * finishes on the terminal's payment screen.
     *
     * Uses multiple strategies for reliability:
     * 1. moveTaskToFront() - most reliable on Android 10+
     * 2. Intent with REORDER_TO_FRONT flag - fallback
     */
    private fun bringToForeground() {
        CCVLogger.logEvent("BRING_TO_FOREGROUND", "Attempting to bring PaymentActivity to foreground")

        try {
            // Strategy 1: Use ActivityManager.moveTaskToFront (most reliable)
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
            CCVLogger.logEvent("BRING_TO_FOREGROUND", "moveTaskToFront called successfully")
        } catch (e: Exception) {
            CCVLogger.logError("BRING_TO_FOREGROUND", "MOVE_TASK_FAILED", e.message, e)
        }

        // Strategy 2: Use Intent as backup (with slight delay to ensure task switch completes)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(this, PaymentActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                CCVLogger.logEvent("BRING_TO_FOREGROUND", "Backup intent launched")
            } catch (e: Exception) {
                CCVLogger.logError("BRING_TO_FOREGROUND", "INTENT_FAILED", e.message, e)
            }
        }, 100)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CCVLogger.logEvent("ON_NEW_INTENT", "PaymentActivity received new intent: ${intent.action}")
        setIntent(intent)
    }

    private fun showSuccessDialog(result: com.example.ccvpayment.model.PaymentResult) {
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
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun formatAmount(amount: BigDecimal): String {
        return "€ ${decimalFormat.format(amount)}"
    }

    override fun onBackPressed() {
        if (binding.loadingOverlay.visibility == View.VISIBLE) {
            // Ödeme sırasında geri tuşunu engelle
            ccv.stopPayment { success ->
                runOnUiThread {
                    if (success) {
                        hideLoading()
                    }
                }
            }
        } else {
            super.onBackPressed()
        }
    }
}
