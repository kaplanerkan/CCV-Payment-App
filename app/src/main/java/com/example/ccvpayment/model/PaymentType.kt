package com.example.ccvpayment.model

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
