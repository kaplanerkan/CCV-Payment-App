package com.example.ccvpayment.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.ccvpayment.R
import com.example.ccvpayment.databinding.ActivitySettingsBinding
import com.example.ccvpayment.helper.CCVPaymentManager
import com.example.ccvpayment.helper.PopupMessageHelper
import com.example.ccvpayment.helper.TerminalHelper
import com.example.ccvpayment.model.TerminalStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.ccvlab.mapi.core.terminal.ExternalTerminal

/**
 * Terminal Settings Activity - Terminal connection and configuration.
 *
 * This activity provides options for configuring the terminal connection
 * and performing terminal-specific operations such as testing connection,
 * starting terminal, and discovering terminals on the network.
 *
 * Features:
 * - IP address and port configuration
 * - Connection testing
 * - Terminal status display
 * - Terminal startup
 * - Repeat last message
 * - Terminal discovery
 *
 * @author Erkan Kaplan
 * @date 2026-02-05
 * @since 1.0
 */
class TerminalSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val ccv = CCVPaymentManager.getInstance()
    private val terminalHelper = TerminalHelper.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        loadTerminalInfo()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        // Bağlantıyı Test Et
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }

        // Terminal Başlat
        binding.btnStartupTerminal.setOnClickListener {
            startupTerminal()
        }

        // Son Mesajı Tekrarla
        binding.btnRepeatLastMessage.setOnClickListener {
            repeatLastMessage()
        }

        // Terminalleri Tara
        binding.btnDiscoverTerminals.setOnClickListener {
            discoverTerminals()
        }
    }

    private fun loadTerminalInfo() {
        binding.tvStatus.text = getString(R.string.terminal_checking)
        binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_yellow)
        binding.tvTerminalId.text = "-"
        binding.tvSoftwareVersion.text = "-"

        val terminal = getTerminalFromInput()

        ccv.checkTerminalStatus(terminal) { status ->
            runOnUiThread {
                updateTerminalInfo(status)
            }
        }
    }

    private fun updateTerminalInfo(status: TerminalStatus?) {
        if (status != null && status.isConnected) {
            binding.tvStatus.text = getString(R.string.terminal_connected)
            binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_green)
            binding.tvTerminalId.text = status.terminalId ?: "-"
            binding.tvSoftwareVersion.text = status.softwareVersion ?: "-"
        } else {
            binding.tvStatus.text = getString(R.string.terminal_disconnected)
            binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_red)
            binding.tvTerminalId.text = "-"
            binding.tvSoftwareVersion.text = "-"
        }
    }

    private fun getTerminalFromInput(): ExternalTerminal {
        val ipAddress = binding.etIpAddress.text?.toString()?.trim() ?: "127.0.0.1"
        val port = binding.etPort.text?.toString()?.toIntOrNull() ?: 20002

        return if (ipAddress == "127.0.0.1" && port == 20002) {
            ccv.getLocalTerminal()
        } else {
            ccv.createTerminal(ipAddress, port)
        }
    }

    private fun testConnection() {
        showLoading(getString(R.string.loading_terminal))

        val terminal = getTerminalFromInput()

        ccv.checkTerminalStatus(terminal) { status ->
            runOnUiThread {
                hideLoading()
                updateTerminalInfo(status)

                if (status != null && status.isConnected) {
                    PopupMessageHelper.showSuccess(this, getString(R.string.connection_success))
                } else {
                    PopupMessageHelper.showError(this, getString(R.string.error_connection_failed))
                }
            }
        }
    }

    private fun startupTerminal() {
        showLoading(getString(R.string.loading_starting_terminal))

        val terminal = getTerminalFromInput()

        ccv.startupTerminal(terminal) { success, errorMessage ->
            runOnUiThread {
                hideLoading()

                if (success) {
                    PopupMessageHelper.showSuccess(this, getString(R.string.terminal_started_success))
                    loadTerminalInfo()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.result_failed))
                        .setMessage(errorMessage ?: getString(R.string.error_unknown))
                        .setPositiveButton(getString(R.string.dialog_ok), null)
                        .show()
                }
            }
        }
    }

    private fun repeatLastMessage() {
        showLoading(getString(R.string.loading_processing))

        val terminal = getTerminalFromInput()

        terminalHelper.repeatLastMessage(terminal, object : TerminalHelper.TerminalOperationCallback {
            override fun onSuccess(message: String?) {
                runOnUiThread {
                    hideLoading()
                    PopupMessageHelper.showSuccess(this@TerminalSettingsActivity, message ?: getString(R.string.last_message_repeated))
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    hideLoading()
                    MaterialAlertDialogBuilder(this@TerminalSettingsActivity)
                        .setTitle(getString(R.string.result_failed))
                        .setMessage(error)
                        .setPositiveButton(getString(R.string.dialog_ok), null)
                        .show()
                }
            }
        })
    }

    private fun discoverTerminals() {
        showLoading(getString(R.string.loading_scanning_terminals))

        terminalHelper.discoverTerminals(object : TerminalHelper.TerminalDiscoveryCallback {
            override fun onTerminalFound(terminal: ExternalTerminal) {
                // Called when each terminal is found
            }

            override fun onDiscoveryComplete(terminals: List<ExternalTerminal>) {
                runOnUiThread {
                    hideLoading()

                    if (terminals.isEmpty()) {
                        MaterialAlertDialogBuilder(this@TerminalSettingsActivity)
                            .setTitle(getString(R.string.discovery_result))
                            .setMessage(getString(R.string.no_terminal_found))
                            .setPositiveButton(getString(R.string.dialog_ok), null)
                            .show()
                    } else {
                        val terminalList = terminals.mapIndexed { index, terminal ->
                            "${index + 1}. ${terminal.ipAddress()}:${terminal.port()}"
                        }.joinToString("\n")

                        MaterialAlertDialogBuilder(this@TerminalSettingsActivity)
                            .setTitle(getString(R.string.terminals_found, terminals.size))
                            .setMessage(terminalList)
                            .setPositiveButton(getString(R.string.dialog_ok), null)
                            .setNeutralButton(getString(R.string.select_first)) { _, _ ->
                                terminals.firstOrNull()?.let { terminal ->
                                    binding.etIpAddress.setText(terminal.ipAddress())
                                    binding.etPort.setText(terminal.port().toString())
                                    testConnection()
                                }
                            }
                            .show()
                    }
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    hideLoading()
                    MaterialAlertDialogBuilder(this@TerminalSettingsActivity)
                        .setTitle(getString(R.string.result_failed))
                        .setMessage(error)
                        .setPositiveButton(getString(R.string.dialog_ok), null)
                        .show()
                }
            }
        })
    }

    private fun showLoading(message: String) {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.tvLoadingMessage.text = message
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }
}
