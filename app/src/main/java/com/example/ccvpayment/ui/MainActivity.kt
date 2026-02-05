package com.example.ccvpayment.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.ccvpayment.helper.PopupMessageHelper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ccvpayment.R
import com.example.ccvpayment.databinding.ActivityMainBinding
import com.example.ccvpayment.helper.CCVLogger
import com.example.ccvpayment.helper.CCVPaymentManager
import com.example.ccvpayment.model.Money
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Main Activity - Home screen of the CCV Payment application.
 *
 * This activity serves as the main menu and provides access to all
 * payment and terminal operations. It displays the current terminal
 * connection status and provides quick access to common functions.
 *
 * Features:
 * - Terminal connection status display
 * - Quick access to Payment and Refund
 * - Period closing (Z-Report)
 * - X-Report (interim report)
 * - Transaction history
 * - Receipt reprinting
 * - Settings access
 *
 * @author Erkan Kaplan
 * @date 2026-02-05
 * @since 1.0
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val ccv = CCVPaymentManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMenuItems()
        setupClickListeners()
        checkTerminalStatus()
    }

    override fun onResume() {
        super.onResume()
        checkTerminalStatus()
    }

    /**
     * Handle new intents when activity is already running (singleTask mode).
     * This is called when the terminal sends back an ECR callback.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Log the ECR callback
        CCVLogger.logEvent("ECR_CALLBACK", mapOf(
            "action" to intent.action,
            "extras" to intent.extras?.keySet()?.map { it to intent.extras?.get(it) }?.toMap()
        ))

        // ECR intent geldiğinde terminal durumunu güncelle
        if (intent.action == "eu.ccv.service.ECR") {
            CCVLogger.logEvent("ECR_RECEIVED", "Terminal callback received in MainActivity")
            checkTerminalStatus()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.app_name)
    }

    private fun setupMenuItems() {
        // Gün Sonu
        setupMenuItem(
            binding.menuPeriodClosing.root,
            R.drawable.ic_report,
            R.string.menu_period_closing,
            R.string.menu_period_closing_subtitle
        )

        // X-Report
        setupMenuItem(
            binding.menuXReport.root,
            R.drawable.ic_report,
            R.string.menu_x_report,
            R.string.menu_x_report_subtitle
        )

        // İşlem Geçmişi
        setupMenuItem(
            binding.menuTransactionHistory.root,
            R.drawable.ic_history,
            R.string.menu_transaction_history,
            R.string.menu_transaction_history_subtitle
        )

        // Son Fişi Yazdır
        setupMenuItem(
            binding.menuReprintTicket.root,
            R.drawable.ic_print,
            R.string.menu_reprint_ticket,
            R.string.menu_reprint_ticket_subtitle
        )

        // Ayarlar
        setupMenuItem(
            binding.menuSettings.root,
            R.drawable.ic_settings,
            R.string.menu_settings,
            R.string.menu_settings_subtitle
        )
    }

    private fun setupMenuItem(view: View, iconRes: Int, titleRes: Int, subtitleRes: Int) {
        view.findViewById<ImageView>(R.id.menuIcon)?.setImageResource(iconRes)
        view.findViewById<TextView>(R.id.menuTitle)?.setText(titleRes)
        view.findViewById<TextView>(R.id.menuSubtitle)?.setText(subtitleRes)
    }

    private fun setupClickListeners() {
        // Ödeme Al
        binding.cardPayment.setOnClickListener {
            startActivity(Intent(this, PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_PAYMENT_TYPE, PaymentActivity.TYPE_SALE)
            })
        }

        // İade
        binding.cardRefund.setOnClickListener {
            startActivity(Intent(this, PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_PAYMENT_TYPE, PaymentActivity.TYPE_REFUND)
            })
        }

        // Gün Sonu (Z-Report)
        binding.menuPeriodClosing.root.setOnClickListener {
            showConfirmDialog(
                title = getString(R.string.confirm_period_closing_title),
                message = getString(R.string.confirm_period_closing_message),
                onConfirm = { performPeriodClosing() }
            )
        }

        // X-Report
        binding.menuXReport.root.setOnClickListener {
            performXReport()
        }

        // İşlem Geçmişi
        binding.menuTransactionHistory.root.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        // Son Fişi Yazdır
        binding.menuReprintTicket.root.setOnClickListener {
            reprintLastTicket()
        }

        // Ayarlar
        binding.menuSettings.root.setOnClickListener {
            startActivity(Intent(this, TerminalSettingsActivity::class.java))
        }

        // Terminal durumu kartına tıkla - durumu yenile
        binding.cardTerminalStatus.setOnClickListener {
            checkTerminalStatus()
        }
    }

    /**
     * Terminal durumunu kontrol et
     */
    private fun checkTerminalStatus() {
        binding.tvTerminalStatus.text = getString(R.string.terminal_checking)
        binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_yellow)
        binding.tvTerminalId.visibility = View.GONE

        val terminal = ccv.getLocalTerminal()

        ccv.checkTerminalStatus(terminal) { status ->
            runOnUiThread {
                if (status != null && status.isConnected) {
                    binding.tvTerminalStatus.text = getString(R.string.terminal_connected)
                    binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_green)

                    status.terminalId?.let { id ->
                        binding.tvTerminalId.text = getString(R.string.terminal_id, id)
                        binding.tvTerminalId.visibility = View.VISIBLE
                    }
                } else {
                    binding.tvTerminalStatus.text = getString(R.string.terminal_disconnected)
                    binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_red)
                    binding.tvTerminalId.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Gün Sonu (Z-Report) işlemi
     */
    private fun performPeriodClosing() {
        showLoading(getString(R.string.loading_period_closing))

        lifecycleScope.launch {
            try {
                val result = ccv.periodClosingAsync()
                hideLoading()

                if (result.success) {
                    showResultDialog(
                        title = getString(R.string.result_success),
                        message = buildString {
                            append(getString(R.string.period_closing_success)).append("\n\n")
                            result.shiftNumber?.let { append(getString(R.string.label_shift, it)).append("\n") }
                            append(getString(R.string.label_total_transactions, result.totalTransactions)).append("\n")
                            append(getString(R.string.label_total_sales, formatMoney(result.totalSalesAmount))).append("\n")
                            append(getString(R.string.label_total_refunds, formatMoney(result.totalRefundsAmount))).append("\n")
                            append(getString(R.string.label_net_amount, formatMoney(result.netAmount)))
                        },
                        isSuccess = true
                    )
                } else {
                    showResultDialog(
                        title = getString(R.string.result_failed),
                        message = result.errorMessage ?: getString(R.string.error_unknown),
                        isSuccess = false
                    )
                }
            } catch (e: Exception) {
                hideLoading()
                showResultDialog(
                    title = getString(R.string.result_failed),
                    message = e.message ?: getString(R.string.error_unknown),
                    isSuccess = false
                )
            }
        }
    }

    /**
     * X-Report işlemi
     */
    private fun performXReport() {
        showLoading(getString(R.string.loading_x_report))

        lifecycleScope.launch {
            try {
                val result = ccv.xReportAsync()
                hideLoading()

                if (result.success) {
                    showResultDialog(
                        title = getString(R.string.x_report_title),
                        message = buildString {
                            append(getString(R.string.x_report_success)).append("\n\n")
                            result.shiftNumber?.let { append(getString(R.string.label_shift, it)).append("\n") }
                            append(getString(R.string.label_total_transactions, result.totalTransactions)).append("\n")
                            append(getString(R.string.label_total_sales, formatMoney(result.totalSalesAmount))).append("\n")
                            append(getString(R.string.label_total_refunds, formatMoney(result.totalRefundsAmount))).append("\n")
                            append(getString(R.string.label_net_amount, formatMoney(result.netAmount)))
                        },
                        isSuccess = true
                    )
                } else {
                    showResultDialog(
                        title = getString(R.string.result_failed),
                        message = result.errorMessage ?: getString(R.string.error_unknown),
                        isSuccess = false
                    )
                }
            } catch (e: Exception) {
                hideLoading()
                showResultDialog(
                    title = getString(R.string.result_failed),
                    message = e.message ?: getString(R.string.error_unknown),
                    isSuccess = false
                )
            }
        }
    }

    /**
     * Son fişi tekrar yazdır
     */
    private fun reprintLastTicket() {
        showLoading(getString(R.string.loading_receipt))

        ccv.reprintLastTicket(
            onResult = { merchantReceipt, customerReceipt ->
                runOnUiThread {
                    hideLoading()

                    val receipt = customerReceipt ?: merchantReceipt
                    if (receipt != null) {
                        showResultDialog(
                            title = getString(R.string.last_receipt_title),
                            message = receipt,
                            isSuccess = true
                        )
                    } else {
                        showResultDialog(
                            title = getString(R.string.result_failed),
                            message = getString(R.string.error_receipt_not_found),
                            isSuccess = false
                        )
                    }
                }
            },
            onError = { errorMessage ->
                runOnUiThread {
                    hideLoading()
                    showResultDialog(
                        title = getString(R.string.result_failed),
                        message = errorMessage,
                        isSuccess = false
                    )
                }
            }
        )
    }

    // ==================== UI HELPERS ====================

    private fun showLoading(message: String) {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.tvLoadingMessage.text = message
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun showConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ -> onConfirm() }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun showResultDialog(
        title: String,
        message: String,
        isSuccess: Boolean
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok), null)
            .setIcon(
                if (isSuccess) R.drawable.ic_payment
                else R.drawable.ic_refund
            )
            .show()
    }

    private fun formatMoney(money: Money?): String {
        return money?.formatted() ?: "€ 0.00"
    }
}
