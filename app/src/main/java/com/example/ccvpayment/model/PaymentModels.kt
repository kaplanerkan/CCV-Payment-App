package com.example.ccvpayment.model

import java.math.BigDecimal
import java.util.Currency
import java.util.Date

/**
 * Ödeme Türleri
 */
enum class PaymentType {
    SALE,       // Satış
    REFUND,     // İade
    REVERSAL,   // İptal
    AUTHORISE,  // Ön Provizyon
    CAPTURE     // Provizyon Kapama
}

/**
 * İşlem Durumları
 */
enum class TransactionStatus {
    SUCCESS,    // Başarılı
    FAILED,     // Başarısız
    CANCELLED,  // İptal Edildi
    PENDING,    // Beklemede
    TIMEOUT,    // Zaman Aşımı
    UNKNOWN     // Bilinmiyor
}

/**
 * Terminal Protokol Türleri
 */
enum class TerminalProtocol {
    OPI_NL,     // OPI-NL protokolü
    OPI_DE      // OPI-DE protokolü
}

/**
 * Socket Modu
 */
enum class SocketMode {
    SINGLE_SOCKET,
    DUAL_SOCKET
}

/**
 * Printer Durumu
 */
enum class PrinterStatus {
    AVAILABLE,
    UNAVAILABLE,
    PAPER_EMPTY,
    PAPER_LOW
}

/**
 * Fiş Modu
 */
enum class ReceiptMode {
    DEFAULT,            // Ödeme sırasında fiş al
    OFF,                // Fiş alma
    RECEIPTS_IN_RESPONSE // Sonuçta fiş al
}

/**
 * Para Birimi Modeli
 */
data class Money(
    val amount: BigDecimal,
    val currency: Currency = Currency.getInstance("EUR")
) {
    fun formatted(): String {
        return String.format("%.2f %s", amount, currency.symbol)
    }
}

/**
 * Ödeme Talebi
 */
data class PaymentRequest(
    val amount: Money,
    val type: PaymentType = PaymentType.SALE,
    val requestId: String = System.currentTimeMillis().toString(),
    val reference: String? = null,
    val allowPartialApproval: Boolean = false,
    val tipAmount: Money? = null
)

/**
 * İade Talebi
 */
data class RefundRequest(
    val amount: Money,
    val originalTransactionId: String? = null,
    val reason: String? = null,
    val requestId: String = System.currentTimeMillis().toString()
)

/**
 * İptal (Reversal) Talebi
 */
data class ReversalRequest(
    val originalRequestId: String,
    val amount: Money? = null
)

/**
 * Ödeme Sonucu
 */
data class PaymentResult(
    val status: TransactionStatus,
    val transactionId: String? = null,
    val requestId: String? = null,
    val amount: Money? = null,
    val approvedAmount: Money? = null,
    val cardBrand: String? = null,
    val maskedPan: String? = null,
    val authCode: String? = null,
    val merchantReceipt: String? = null,
    val customerReceipt: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val timestamp: Date = Date()
) {
    val isSuccess: Boolean get() = status == TransactionStatus.SUCCESS
    val isPartialApproval: Boolean get() = approvedAmount != null && amount != null && approvedAmount.amount < amount.amount
}

/**
 * Terminal Bilgisi
 */
data class TerminalInfo(
    val ipAddress: String,
    val port: Int,
    val compatibilityPort: Int,
    val protocol: TerminalProtocol,
    val socketMode: SocketMode,
    val terminalId: String? = null,
    val softwareVersion: String? = null,
    val isConnected: Boolean = false
)

/**
 * Terminal Durum Bilgisi
 */
data class TerminalStatus(
    val isConnected: Boolean,
    val terminalId: String? = null,
    val softwareVersion: String? = null,
    val ipAddress: String? = null,
    val batteryLevel: Int? = null,
    val printerStatus: PrinterStatus? = null,
    val lastTransactionTime: Date? = null
)

/**
 * İşlem Geçmişi Bilgisi
 */
data class TransactionInfo(
    val transactionId: String,
    val type: PaymentType,
    val status: TransactionStatus,
    val amount: Money,
    val cardBrand: String?,
    val maskedPan: String?,
    val timestamp: Date,
    val shiftNumber: Int?,
    val authCode: String?
)

/**
 * Dönem Kapama Sonucu (Z-Raporu / X-Raporu)
 */
data class PeriodClosingResult(
    val success: Boolean,
    val shiftNumber: Int?,
    val totalTransactions: Int,
    val totalSalesCount: Int,
    val totalSalesAmount: Money?,
    val totalRefundsCount: Int,
    val totalRefundsAmount: Money?,
    val netAmount: Money?,
    val receipt: String?,
    val closingTime: Date = Date(),
    val errorMessage: String? = null
)

/**
 * İşlem Özeti
 */
data class TransactionOverviewResult(
    val success: Boolean,
    val shiftNumber: Int?,
    val transactions: List<TransactionInfo>,
    val errorMessage: String? = null
)

/**
 * Fiş Bilgisi
 */
data class ReceiptInfo(
    val merchantReceipt: String?,
    val customerReceipt: String?,
    val journalReceipt: String?
)
