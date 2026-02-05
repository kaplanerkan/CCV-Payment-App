package com.example.ccvpayment.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ccvpayment.R
import com.example.ccvpayment.databinding.ActivityTransactionHistoryBinding
import com.example.ccvpayment.helper.CCVPaymentManager
import com.example.ccvpayment.model.PaymentType
import com.example.ccvpayment.model.TransactionInfo
import com.example.ccvpayment.model.TransactionStatus
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * İşlem Geçmişi Activity
 *
 * Tüm yapılan işlemleri listeler.
 */
class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private val ccv = CCVPaymentManager.getInstance()
    private val adapter = TransactionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadTransactions()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadTransactions() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val result = ccv.getTransactionHistoryAsync()
                binding.loadingOverlay.visibility = View.GONE

                if (result.success && result.transactions.isNotEmpty()) {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                    adapter.submitList(result.transactions)
                    updateSummary(result.transactions)
                } else {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.cardSummary.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.loadingOverlay.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.cardSummary.visibility = View.GONE

                MaterialAlertDialogBuilder(this@TransactionHistoryActivity)
                    .setTitle(getString(R.string.result_failed))
                    .setMessage(e.message ?: getString(R.string.error_unknown))
                    .setPositiveButton(getString(R.string.dialog_ok), null)
                    .show()
            }
        }
    }

    private fun updateSummary(transactions: List<TransactionInfo>) {
        binding.cardSummary.visibility = View.VISIBLE

        val totalCount = transactions.size
        val totalAmount = transactions
            .filter { it.status == TransactionStatus.SUCCESS }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount.amount) }

        binding.tvTotalTransactions.text = totalCount.toString()
        binding.tvTotalAmount.text = formatAmount(totalAmount)
    }

    private fun formatAmount(amount: BigDecimal): String {
        val format = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.GERMANY))
        return "€${format.format(amount)}"
    }

    // ==================== ADAPTER ====================

    inner class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        private var items: List<TransactionInfo> = emptyList()

        fun submitList(list: List<TransactionInfo>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val ivTransactionType: ImageView = itemView.findViewById(R.id.ivTransactionType)
            private val tvTransactionType: TextView = itemView.findViewById(R.id.tvTransactionType)
            private val tvCardBrand: TextView = itemView.findViewById(R.id.tvCardBrand)
            private val tvMaskedPan: TextView = itemView.findViewById(R.id.tvMaskedPan)
            private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
            private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

            private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

            fun bind(item: TransactionInfo) {
                // İşlem tipi
                val (typeText, typeIcon, typeColor) = when (item.type) {
                    PaymentType.REFUND -> Triple(
                        getString(R.string.payment_type_refund),
                        R.drawable.ic_refund,
                        R.color.payment_refund
                    )
                    PaymentType.REVERSAL -> Triple(
                        getString(R.string.payment_type_reversal),
                        R.drawable.ic_refund,
                        R.color.payment_reversal
                    )
                    else -> Triple(
                        getString(R.string.payment_type_sale),
                        R.drawable.ic_payment,
                        R.color.primary
                    )
                }

                tvTransactionType.text = typeText
                ivTransactionType.setImageResource(typeIcon)

                // Kart bilgisi
                tvCardBrand.text = item.cardBrand ?: "-"
                tvMaskedPan.text = item.maskedPan ?: "****"

                // Zaman
                tvTimestamp.text = dateFormat.format(item.timestamp)

                // Tutar
                tvAmount.text = formatAmount(item.amount.amount)

                // Durum
                val (statusText, statusColor) = when (item.status) {
                    TransactionStatus.SUCCESS -> getString(R.string.transaction_status_success) to R.color.status_success
                    TransactionStatus.FAILED -> getString(R.string.transaction_status_failed) to R.color.status_error
                    TransactionStatus.CANCELLED -> getString(R.string.transaction_status_cancelled) to R.color.status_warning
                    else -> getString(R.string.transaction_status_pending) to R.color.text_secondary
                }

                tvStatus.text = statusText
                tvStatus.setTextColor(getColor(statusColor))

                // Tıklama
                (itemView as MaterialCardView).setOnClickListener {
                    showTransactionDetails(item)
                }
            }
        }
    }

    private fun showTransactionDetails(item: TransactionInfo) {
        val typeText = when (item.type) {
            PaymentType.SALE -> getString(R.string.payment_type_sale)
            PaymentType.REFUND -> getString(R.string.payment_type_refund)
            PaymentType.REVERSAL -> getString(R.string.payment_type_reversal)
            PaymentType.AUTHORISE -> getString(R.string.payment_type_authorise)
            PaymentType.CAPTURE -> getString(R.string.payment_type_capture)
        }

        val message = buildString {
            append("İşlem Tipi: $typeText\n")
            append("İşlem No: ${item.transactionId}\n")
            append("Tutar: ${formatAmount(item.amount.amount)}\n")
            item.cardBrand?.let { append("Kart: $it\n") }
            item.maskedPan?.let { append("Kart No: $it\n") }
            item.authCode?.let { append("Onay Kodu: $it\n") }
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            append("Tarih: ${format.format(item.timestamp)}")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("İşlem Detayı")
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_close), null)
            .show()
    }
}
