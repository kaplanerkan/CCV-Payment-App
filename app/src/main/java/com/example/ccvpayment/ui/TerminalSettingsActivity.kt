package com.example.ccvpayment.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ccvpayment.R
import com.example.ccvpayment.databinding.ActivitySettingsBinding
import com.example.ccvpayment.helper.CCVPaymentManager
import com.example.ccvpayment.helper.TerminalHelper
import com.example.ccvpayment.model.TerminalStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.ccvlab.mapi.core.terminal.ExternalTerminal

/**
 * Terminal Ayarları Activity
 *
 * Terminal bağlantı ayarları ve terminal işlemleri.
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
                    Toast.makeText(this, "Bağlantı başarılı!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.error_connection_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startupTerminal() {
        showLoading("Terminal başlatılıyor...")

        val terminal = getTerminalFromInput()

        ccv.startupTerminal(terminal) { success, errorMessage ->
            runOnUiThread {
                hideLoading()

                if (success) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.result_success))
                        .setMessage("Terminal başarıyla başlatıldı")
                        .setPositiveButton(getString(R.string.dialog_ok), null)
                        .show()
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
        showLoading("İşlem yapılıyor...")

        val terminal = getTerminalFromInput()

        terminalHelper.repeatLastMessage(terminal, object : TerminalHelper.TerminalOperationCallback {
            override fun onSuccess(message: String?) {
                runOnUiThread {
                    hideLoading()
                    Toast.makeText(this@TerminalSettingsActivity, message ?: "Son mesaj tekrarlandı", Toast.LENGTH_SHORT).show()
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
        showLoading("Terminaller taranıyor...")

        terminalHelper.discoverTerminals(object : TerminalHelper.TerminalDiscoveryCallback {
            override fun onTerminalFound(terminal: ExternalTerminal) {
                // Her terminal bulunduğunda çağrılır
            }

            override fun onDiscoveryComplete(terminals: List<ExternalTerminal>) {
                runOnUiThread {
                    hideLoading()

                    if (terminals.isEmpty()) {
                        MaterialAlertDialogBuilder(this@TerminalSettingsActivity)
                            .setTitle("Sonuç")
                            .setMessage("Hiç terminal bulunamadı")
                            .setPositiveButton(getString(R.string.dialog_ok), null)
                            .show()
                    } else {
                        val terminalList = terminals.mapIndexed { index, terminal ->
                            "${index + 1}. ${terminal.ipAddress()}:${terminal.port()}"
                        }.joinToString("\n")

                        MaterialAlertDialogBuilder(this@TerminalSettingsActivity)
                            .setTitle("Bulunan Terminaller (${terminals.size})")
                            .setMessage(terminalList)
                            .setPositiveButton(getString(R.string.dialog_ok), null)
                            .setNeutralButton("İlkini Seç") { _, _ ->
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
